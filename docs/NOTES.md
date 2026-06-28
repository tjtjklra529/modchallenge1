Developer notes:
- The LuckPerms adapter uses reflection; test on target LuckPerms versions.
- Web server uses com.sun.net.httpserver.HttpServer; consider replacing with Jetty for production.
- Delivery store persists JSONL lines; rotate logs if large.
