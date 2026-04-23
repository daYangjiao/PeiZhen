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
- `scripts/install-autodeploy.sh`: install the Gitee WebHook receiver, Node 20, optional Linux HBuilderX CLI, systemd service, and Nginx route.
- `scripts/autodeploy-run.sh`: server-side deploy runner used by the WebHook service to publish backend, H5/admin, and WGT from Gitee `FF`.
- `autodeploy/webhook_server.py`: stdlib Python WebHook receiver that validates `X-Gitee-Token` and only accepts `refs/heads/FF`.

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

Gitee push-to-deploy:

1. On the server, install the WebHook service:

```bash
sudo bash deploy/scripts/install-autodeploy.sh
```

2. Edit the server-only env file:

```bash
sudo -u ops vim /etc/pz-autodeploy/autodeploy.env
```

Set `HBUILDERX_USERNAME` and `HBUILDERX_PASSWORD`. `HBUILDERX_DOWNLOAD_URL` can stay blank; the installer resolves the current Linux package from DCloud release metadata. Keep the file mode `600`; the installer enforces this.

3. Add a Gitee WebHook:

```text
URL: http://101.245.94.141/gitee-webhook
Password/Token: value of GITEE_WEBHOOK_TOKEN in /etc/pz-autodeploy/autodeploy.env
Events: Push
```

Behavior:

- Only pushes to `FF` deploy production.
- Pushes to `main` or `master` are acknowledged and ignored.
- A deployment lock prevents overlapping deploys.
- Backend and H5/admin publish first. WGT publishes last.
- WGT uses Linux HBuilderX CLI with `publish app --type wgt --project <absolute project path>`.
- If WGT generation fails, backend and H5 stay deployed; check `/var/log/pz-autodeploy/` and `journalctl -u pz-autodeploy-webhook`.

WGT versioning:

```text
wgtVersion=<manifest.versionName>-wgt.<shortCommit>
file=/opt/pz-app/uploads/app-updates/android/yuanban-<wgtVersion>.wgt
metadata=/opt/pz-app/uploads/app-updates/android.json
```

Validate automatic deployment:

```bash
systemctl status pz-autodeploy-webhook --no-pager
curl -s http://127.0.0.1:9017/gitee-webhook/healthz
cat /var/lib/pz-deploy/releases/backend-release.env
cat /var/lib/pz-deploy/releases/frontend-release.env
cat /var/lib/pz-deploy/releases/wgt-release.env
cat /opt/pz-app/uploads/app-updates/android.json
```

Team collaboration with GitHub:

- Use `GitHub` as the source of truth for code review and merges.
- Add collaborators directly in the GitHub repository settings.
- Daily workflow:
  1. Everyone pulls from GitHub.
  2. Everyone works on their own branch and opens PRs.
  3. Merge to `FF` only after review.
- This repo now includes `.github/workflows/deploy-ff.yml`.
- Behavior:
  - Every push to `FF` auto-deploys backend + H5.
  - GitHub Actions also supports manual publish with `all / backend / frontend`.
- Required GitHub repository secret:

```bash
DEPLOY_SSH_KEY=<ops user private key content>
```

- Recommended repository settings:
  - Protect `FF`
  - Require PR review before merge
  - Restrict direct pushes if you want release control

With this setup, collaborators only need GitHub access to trigger backend/H5 deployment. They do not need to run publish scripts on your Mac.

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

App update delivery:

- H5 stays server-hosted and updates immediately after frontend publish.
- Native App keeps bundled pages; installed clients do not read the server H5 automatically.
- App updates are now split into:
  - `101`: WGT resource update for JS/CSS/pages/static assets
  - `102`: full Android APK update for native manifest/permissions/plugins/icon/splash changes

Backend update check endpoint:

```bash
POST /api/app-upgrade/check
```

The backend reads update metadata from:

```bash
${APP_UPLOAD_DIR}/app-updates/android.json
```

Publish update artifacts to the server:

```bash
APP_VERSION=1.0.1 \
APP_VERSION_CODE=101 \
APK_FILE=/absolute/path/to/app.apk \
WGT_FILE=/absolute/path/to/app.wgt \
WGT_VERSION=1.0.1-hotfix.1 \
UPDATE_NOTES='修复头像显示与订单时间线问题' \
bash deploy/scripts/publish-app-update.sh
```

The metadata format example is:

```bash
deploy/app-updates/android.example.json
```

Check local/remote consistency in one command:

```bash
bash deploy/scripts/check-consistency.sh
```

WGT collaboration note:

- `WGT` is still a packaged build artifact, not a git-tracked source output.
- Current project already supports publishing a packaged WGT from any collaborator machine with:

```bash
APP_VERSION=1.0.1 \
APP_VERSION_CODE=101 \
WGT_FILE=/absolute/path/to/app.wgt \
WGT_VERSION=1.0.1-hotfix.1 \
UPDATE_NOTES='修复首页与 AI 预约交互' \
bash deploy/scripts/publish-app-update.sh
```

- This means:
  - `backend + h5` can be auto-published through GitHub Actions
  - `wgt` can be published by any collaborator who has the packaged file and the same `ops` deploy key
- If you want, the next step can be to continue turning `wgt` into a GitHub Actions release pipeline too, but that requires settling the CI-side app packaging toolchain first.
