# Huawei Cloud Ubuntu 24.04 Deployment

This directory contains the server-side templates needed to run the current stack:

- `scripts/server-bootstrap.sh`: install Java 17, Maven, MySQL, Nginx, create the `pzapp` runtime user, and prepare directories.
- `scripts/harden-server.sh`: create the `ops` SSH user, install the SSH public key, grant `sudo`, and optionally disable root/password SSH login.
- `env/pz-app.env.example`: environment variables for the Spring Boot service.
- `systemd/pz-app.service`: service unit that runs the backend as `pzapp`.
- `nginx/pz-mini.conf`: Nginx site that serves the H5 build on `/` and proxies API, upload, and WebSocket traffic to `127.0.0.1:8080`.
- `scripts/deploy-backend.sh`: package and publish the backend jar to `/opt/pz-app/app/pz-app.jar`.
- `scripts/deploy-frontend.sh`: sync the H5 build output into `/var/www/pz-mini`.
- `scripts/publish-frontend.sh`: build the H5 app on macOS, sync it to the server, and run the frontend deploy step remotely.
- `scripts/publish-backend.sh`: sync the repo to the server and run the backend deploy step remotely.
- `scripts/publish-all.sh`: publish backend first and frontend second using the same local git source version.
- `scripts/publish-lib.sh`: shared helpers for git-based release metadata and rollback-by-ref publishing.

Recommended server flow:

```bash
ssh ops@server-ip
cd ~/PZ_yuanbao
sudo bash deploy/scripts/server-bootstrap.sh
sudo OPS_PUBLIC_KEY="$(cat ~/.ssh/pz_ops_ed25519.pub)" bash deploy/scripts/harden-server.sh
sudo cp deploy/env/pz-app.env.example /etc/pz-app/pz-app.env
sudo vim /etc/pz-app/pz-app.env
mysql -uroot -p -e "CREATE DATABASE IF NOT EXISTS student DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -uroot -p student < database/student.sql
sudo bash deploy/scripts/deploy-backend.sh
sudo cp deploy/nginx/pz-mini.conf /etc/nginx/sites-available/pz-mini.conf
sudo ln -sf /etc/nginx/sites-available/pz-mini.conf /etc/nginx/sites-enabled/pz-mini.conf
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t && sudo systemctl reload nginx
```

SSH recommendation:

- Use a dedicated `ops` user with SSH key login and `sudo`.
- Keep the Spring Boot runtime user as `pzapp`.
- After verifying `ops` login works, re-run the hardening script with `APPLY_SSH_HARDENING=1` to disable direct root login and password login in SSH.

H5 build upload:

1. Build `frontend/mini-program` into H5 on macOS.
2. Copy the build output to the server.
3. Run `sudo bash deploy/scripts/deploy-frontend.sh <h5-dist-path>`.

The frontend deploy script also syncs `frontend/mini-program/static/` into the web root so legacy `/static/...` references keep working during the H5 cleanup period.

One-command publish from your Mac:

```bash
bash deploy/scripts/publish-frontend.sh
bash deploy/scripts/publish-backend.sh
bash deploy/scripts/publish-all.sh
```

Release discipline:

- Treat the local git repo as the only source of truth.
- Publish from a clean tracked worktree by default.
- Every publish records commit, branch/ref, dirty-state, timestamp, and operator under `/var/lib/pz-deploy/releases/`.
- Use `ALLOW_DIRTY=1` only for emergency testing, not for normal production releases.

Rollback by git tag or commit:

```bash
GIT_REF=<tag-or-commit> bash deploy/scripts/publish-frontend.sh
GIT_REF=<tag-or-commit> bash deploy/scripts/publish-backend.sh
```

This creates a temporary git worktree for the requested ref, publishes that exact version, and then removes the temporary workspace automatically.

Optional environment overrides:

```bash
SERVER_HOST=101.245.94.141 \
SERVER_USER=ops \
SSH_KEY=$HOME/.ssh/pz_ops_ed25519 \
REMOTE_ROOT=/home/ops/PZ_yuanbao \
bash deploy/scripts/publish-frontend.sh
```

WeChat mini-program login placeholders:

- The unified login page now shows the WeChat entry button on both H5 and MP-WEIXIN.
- Before real WeChat login is enabled, both ends will display the backend-provided reason, currently `微信登录暂未开通`.
- To enable real mini-program login later, set these variables in `/etc/pz-app/pz-app.env` and redeploy the backend:

```bash
WECHAT_MINI_APP_ID=your-mini-app-id
WECHAT_MINI_APP_SECRET=your-mini-app-secret
WECHAT_BIND_TOKEN_EXPIRATION_MS=600000
```

After that, complete the remaining platform setup before opening the button for real traffic:

- ICP/filed domain ready
- HTTPS available
- WeChat request/download/socket legal domains configured
- Mini-program AppID/Secret verified against the same project

Validation:

```bash
systemctl status pz-app
journalctl -u pz-app -n 100 --no-pager
cat /var/lib/pz-deploy/releases/backend-release.env
cat /var/lib/pz-deploy/releases/frontend-release.env
curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8080/swagger-ui/index.html
curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1/
ss -lntp | grep -E ':80|:8080|:3306'
```

Android APK internal test:

1. Open `frontend/mini-program` in HBuilderX.
2. Choose `发行 -> 原生App-云打包`.
3. Build target: `Android`.
4. Package name: `cn.yuanban.peizhen`.
5. App icon source: `frontend/mini-program/static/brand-logo.png`.
6. The APK is configured to connect directly to:
   - API: `http://101.245.94.141`
   - WebSocket: `ws://101.245.94.141`

After installation, the APK runs independently and does not require the developer machine to stay online.
