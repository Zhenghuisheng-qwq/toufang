"""隔离 PoC 静态服务：页面走文件，/api 固定返回 401。不连接正式后端。"""
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
import json


class Handler(SimpleHTTPRequestHandler):
    def do_GET(self):
        if self.path.startswith("/api/"):
            self.respond_unauthorized()
            return
        super().do_GET()

    def do_POST(self):
        if self.path.startswith("/api/"):
            self.respond_unauthorized()
            return
        self.send_error(404)

    def respond_unauthorized(self):
        body = json.dumps({"code": 401, "message": "未登录"}, ensure_ascii=False).encode("utf-8")
        self.send_response(401)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)


if __name__ == "__main__":
    ThreadingHTTPServer(("127.0.0.1", 8093), Handler).serve_forever()
