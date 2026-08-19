#!/usr/bin/env python3
"""Serve AAC Text Tiles iPad to the iPad over your own WiFi.

  http://<pc-ip>:8080/   -> setup page + the certificate the iPad needs
  https://<pc-ip>:8443/  -> the app itself (install this one to the Home Screen)

Nothing leaves your network. Once the iPad has added the app to its Home Screen,
this computer can be turned off - the app runs from the iPad's own storage.
"""
import http.server, socketserver, ssl, socket, subprocess, threading, os, sys, functools

APP_DIR  = os.path.dirname(os.path.abspath(__file__))
CERT_DIR = os.path.join(APP_DIR, 'certs')
SETUP_DIR = os.path.join(CERT_DIR, 'setup')
HTTP_PORT, HTTPS_PORT = 8080, 8443


def lan_ip():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        s.connect(('1.1.1.1', 80))
        return s.getsockname()[0]
    finally:
        s.close()


def make_certs(ip):
    """Create a small private certificate authority and a server certificate for this PC's IP."""
    os.makedirs(SETUP_DIR, exist_ok=True)
    ca_key, ca_crt = f'{CERT_DIR}/ca.key', f'{SETUP_DIR}/AAC Text Tiles iPad-CA.crt'
    sv_key, sv_crt = f'{CERT_DIR}/server.key', f'{CERT_DIR}/server.crt'
    marker = os.path.join(CERT_DIR, 'issued_for.txt')
    same_ip = os.path.exists(marker) and open(marker).read().strip() == ip
    if os.path.exists(sv_crt) and os.path.exists(ca_crt) and same_ip:
        return ca_crt, sv_key, sv_crt

    print('Creating certificates for ' + ip + ' ...')
    run = lambda c: subprocess.run(c, check=True, capture_output=True)

    # Extensions go through a config file rather than -addext: macOS ships
    # LibreSSL, which had no -addext until 3.1, so -addext fails outright there.
    cfg = os.path.join(CERT_DIR, 'openssl.cnf')
    with open(cfg, 'w') as f:
        f.write('[req]\n'
                'distinguished_name = dn\n'
                'prompt = no\n'
                '[dn]\n'
                'CN = AAC Text Tiles iPad Local CA\n'
                '[ca_ext]\n'
                'basicConstraints = critical,CA:TRUE\n'
                'keyUsage = critical,keyCertSign,cRLSign\n')
    run(['openssl', 'req', '-x509', '-newkey', 'rsa:2048', '-nodes', '-days', '3650',
         '-keyout', ca_key, '-out', ca_crt, '-config', cfg, '-extensions', 'ca_ext'])
    run(['openssl', 'req', '-newkey', 'rsa:2048', '-nodes', '-config', cfg,
         '-keyout', sv_key, '-out', f'{CERT_DIR}/server.csr', '-subj', '/CN=' + ip])
    ext = os.path.join(CERT_DIR, 'server.ext')
    with open(ext, 'w') as f:
        f.write('basicConstraints=CA:FALSE\n'
                'keyUsage=critical,digitalSignature,keyEncipherment\n'
                'extendedKeyUsage=serverAuth\n'
                'subjectAltName=IP:' + ip + '\n')
    run(['openssl', 'x509', '-req', '-in', f'{CERT_DIR}/server.csr', '-CA', ca_crt,
         '-CAkey', ca_key, '-CAcreateserial', '-out', sv_crt, '-days', '397',
         '-sha256', '-extfile', ext])
    open(marker, 'w').write(ip)
    return ca_crt, sv_key, sv_crt


SETUP_PAGE = """<!DOCTYPE html><html><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>Install AAC Text Tiles iPad</title>
<style>body{{font:17px/1.5 -apple-system,sans-serif;max-width:640px;margin:0 auto;padding:24px;
background:#141a22;color:#eef3f8}} h1{{font-size:26px}} a.btn{{display:block;text-align:center;
background:#1f9d55;color:#fff;padding:18px;border-radius:14px;text-decoration:none;font-weight:700;
margin:18px 0;font-size:19px}} ol{{padding-left:22px}} li{{margin-bottom:14px}}
code{{background:#26313f;padding:2px 6px;border-radius:6px}}</style></head><body>
<h1>Put AAC Text Tiles iPad on this iPad</h1>
<p>Three steps, once. After this the app lives on the iPad and works with no internet.</p>
<ol>
<li><b>Download the certificate</b> - this is what lets the iPad trust your own computer.
<a class="btn" href="/AAC Text Tiles iPad-CA.crt">1. Download certificate</a>
Then open <b>Settings</b> - a line saying <i>Profile Downloaded</i> appears near the top.
Tap it, then <b>Install</b> (top right), enter the passcode, <b>Install</b> again.</li>
<li><b>Turn the trust on:</b> Settings &rarr; General &rarr; About &rarr; scroll to the bottom &rarr;
<b>Certificate Trust Settings</b> &rarr; switch on <b>AAC Text Tiles iPad Local CA</b>.</li>
<li><b>Open the app and keep it:</b>
<a class="btn" href="https://{ip}:{hp}/">2. Open AAC Text Tiles iPad</a>
Then tap the <b>Share</b> button (the box with the arrow) &rarr; <b>Add to Home Screen</b> &rarr; <b>Add</b>.</li>
</ol>
<p>Now close Safari and open <b>AAC Text Tiles iPad</b> from the Home Screen. No address bar, no internet needed.</p>
</body></html>"""


class DualStack(socketserver.ThreadingTCPServer):
    """Answers on one port whether Safari asks in http or https.

    iOS quietly rewrites a typed address to https://, which a plain HTTP port
    refuses - that shows up as "Safari cannot open the page". Peeking at the
    first byte (0x16 starts a TLS handshake) lets one port serve both."""
    allow_reuse_address = True
    daemon_threads = True

    def __init__(self, addr, handler, ctx=None):
        self.ctx = ctx
        super().__init__(addr, handler)

    def get_request(self):
        sock, addr = self.socket.accept()
        if self.ctx is not None:
            try:
                sock.settimeout(8)
                if sock.recv(1, socket.MSG_PEEK) == b'\x16':
                    sock = self.ctx.wrap_socket(sock, server_side=True)
                sock.settimeout(None)
            except Exception:
                pass
        return sock, addr


class Quiet(http.server.SimpleHTTPRequestHandler):
    def log_message(self, fmt, *a):
        sys.stdout.write('  %s %s\n' % (self.address_string(), fmt % a))

    def end_headers(self):
        # Without this Safari keeps its own copy of app.js and quietly ignores a
        # newer one, so an updated app never reaches the iPad. The service worker
        # is what makes the app offline; the browser cache must stay out of it.
        self.send_header('Cache-Control', 'no-store, must-revalidate')
        super().end_headers()


def tls_context(key, crt):
    ctx = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
    ctx.load_cert_chain(crt, key)
    return ctx


def serve_setup(key, crt):
    """Setup page + certificate download. Answers http or https on the same port."""
    Quiet.extensions_map = dict(http.server.SimpleHTTPRequestHandler.extensions_map)
    Quiet.extensions_map['.crt'] = 'application/x-x509-ca-cert'
    handler = functools.partial(Quiet, directory=SETUP_DIR)
    DualStack(('', HTTP_PORT), handler, tls_context(key, crt)).serve_forever()


def serve_app(key, crt):
    """The app itself - https only, because that is what makes Safari keep it offline."""
    handler = functools.partial(Quiet, directory=APP_DIR)
    httpd = DualStack(('', HTTPS_PORT), handler)
    httpd.socket = tls_context(key, crt).wrap_socket(httpd.socket, server_side=True)
    httpd.serve_forever()


def main():
    ip = lan_ip()
    ca, key, crt = make_certs(ip)
    with open(os.path.join(SETUP_DIR, 'index.html'), 'w') as f:
        f.write(SETUP_PAGE.format(ip=ip, hp=HTTPS_PORT))
    threading.Thread(target=serve_setup, args=(key, crt), daemon=True).start()
    print('\n' + '=' * 58)
    print('  On the iPad, open Safari and go to:')
    print('     http://%s:%d' % (ip, HTTP_PORT))
    print('  and follow the three steps on that page.')
    print('=' * 58)
    print('  If Safari will not open it, try https://%s:%d - same page.' % (ip, HTTP_PORT))
    print('  Ctrl+C here when the iPad says it is installed.\n')
    try:
        serve_app(key, crt)
    except KeyboardInterrupt:
        print('\nStopped.')


if __name__ == '__main__':
    main()
