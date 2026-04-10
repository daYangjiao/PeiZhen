package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.example.common.ResponseResult;
import org.example.model.request.AppUpgradeCheckRequest;
import org.example.model.response.AppUpgradeCheckResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

@RestController
@RequestMapping("/api/app-upgrade")
@Api(tags = "App 升级接口")
public class AppUpgradeController {

    private static final String DEFAULT_ANDROID_PLATFORM = "android";

    @Value("${app.upload-dir:${user.dir}/uploads}")
    private String uploadDir;

    private final ObjectMapper objectMapper;

    public AppUpgradeController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @PostMapping("/check")
    @ApiOperation("检查 App 更新")
    public ResponseResult<AppUpgradeCheckResponse> check(@RequestBody(required = false) AppUpgradeCheckRequest request,
                                                         HttpServletRequest httpServletRequest) {
        AppUpgradeCheckResponse response = new AppUpgradeCheckResponse();
        if (request == null) {
            return ResponseResult.success(response);
        }

        String platform = normalizePlatform(request.getPlatform());
        if (!DEFAULT_ANDROID_PLATFORM.equals(platform)) {
            return ResponseResult.success(response);
        }

        UpgradeMetadata metadata = loadMetadata(platform);
        if (metadata == null) {
            return ResponseResult.success(response);
        }

        response.setTitle(StringUtils.hasText(metadata.title) ? metadata.title : "发现新版本");
        response.setNotes(StringUtils.hasText(metadata.notes) ? metadata.notes : "");
        response.setForceUpdate(Boolean.TRUE.equals(metadata.forceUpdate));
        response.setLatestVersion(metadata.latestVersion);
        response.setLatestVersionCode(metadata.latestVersionCode);
        response.setWgtVersion(metadata.wgtVersion);

        if (hasApkUpgrade(request, metadata)) {
            response.setUpdateType(AppUpgradeCheckResponse.UPDATE_APK);
            response.setDownloadUrl(resolvePublicUrl(httpServletRequest, metadata.apkUrl, metadata.apkPath));
            return ResponseResult.success(response);
        }

        if (hasWgtUpgrade(request, metadata)) {
            response.setUpdateType(AppUpgradeCheckResponse.UPDATE_WGT);
            response.setDownloadUrl(resolvePublicUrl(httpServletRequest, metadata.wgtUrl, metadata.wgtPath));
        }

        return ResponseResult.success(response);
    }

    private String normalizePlatform(String platform) {
        return StringUtils.hasText(platform) ? platform.trim().toLowerCase(Locale.ROOT) : "";
    }

    private UpgradeMetadata loadMetadata(String platform) {
        Path metadataPath = Paths.get(uploadDir).resolve("app-updates").resolve(platform + ".json").normalize();
        if (!Files.exists(metadataPath)) {
            return null;
        }
        try {
            return objectMapper.readValue(metadataPath.toFile(), UpgradeMetadata.class);
        } catch (IOException e) {
            throw new IllegalStateException("读取升级配置失败: " + e.getMessage(), e);
        }
    }

    private boolean hasApkUpgrade(AppUpgradeCheckRequest request, UpgradeMetadata metadata) {
        if (!StringUtils.hasText(metadata.apkUrl) && !StringUtils.hasText(metadata.apkPath)) {
            return false;
        }
        Integer latestVersionCode = metadata.latestVersionCode;
        Integer currentVersionCode = request.getAppVersionCode();
        if (latestVersionCode != null && currentVersionCode != null) {
            return latestVersionCode > currentVersionCode;
        }
        return compareVersion(metadata.latestVersion, request.getAppVersion()) > 0;
    }

    private boolean hasWgtUpgrade(AppUpgradeCheckRequest request, UpgradeMetadata metadata) {
        if (!StringUtils.hasText(metadata.wgtVersion)) {
            return false;
        }
        if (!StringUtils.hasText(metadata.wgtUrl) && !StringUtils.hasText(metadata.wgtPath)) {
            return false;
        }
        String currentWgtVersion = StringUtils.hasText(request.getWgtVersion()) ? request.getWgtVersion().trim() : "";
        if (!StringUtils.hasText(currentWgtVersion)) {
            currentWgtVersion = StringUtils.hasText(request.getAppVersion()) ? request.getAppVersion().trim() : "";
        }
        return !metadata.wgtVersion.trim().equals(currentWgtVersion);
    }

    private String resolvePublicUrl(HttpServletRequest request, String absoluteUrl, String relativePath) {
        if (StringUtils.hasText(absoluteUrl)) {
            return absoluteUrl.trim();
        }
        if (!StringUtils.hasText(relativePath)) {
            return "";
        }
        String normalizedPath = relativePath.startsWith("/") ? relativePath : "/" + relativePath;
        String host = request.getServerName();
        boolean defaultPort = ("http".equalsIgnoreCase(request.getScheme()) && request.getServerPort() == 80)
                || ("https".equalsIgnoreCase(request.getScheme()) && request.getServerPort() == 443);
        String portPart = defaultPort ? "" : ":" + request.getServerPort();
        return request.getScheme() + "://" + host + portPart + normalizedPath;
    }

    private int compareVersion(String left, String right) {
        if (!StringUtils.hasText(left) && !StringUtils.hasText(right)) return 0;
        if (!StringUtils.hasText(left)) return -1;
        if (!StringUtils.hasText(right)) return 1;

        String[] leftParts = left.split("[.-]");
        String[] rightParts = right.split("[.-]");
        int length = Math.max(leftParts.length, rightParts.length);
        for (int i = 0; i < length; i++) {
            String l = i < leftParts.length ? leftParts[i] : "0";
            String r = i < rightParts.length ? rightParts[i] : "0";
            int cmp = compareVersionToken(l, r);
            if (cmp != 0) return cmp;
        }
        return 0;
    }

    private int compareVersionToken(String left, String right) {
        boolean leftNumeric = left.matches("\\d+");
        boolean rightNumeric = right.matches("\\d+");
        if (leftNumeric && rightNumeric) {
            return Integer.compare(Integer.parseInt(left), Integer.parseInt(right));
        }
        return left.compareTo(right);
    }

    public static class UpgradeMetadata {
        public String title;
        public String notes;
        public Boolean forceUpdate;
        public String latestVersion;
        public Integer latestVersionCode;
        public String apkUrl;
        public String apkPath;
        public String wgtVersion;
        public String wgtUrl;
        public String wgtPath;
    }
}
