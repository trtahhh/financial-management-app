package com.example.finance.service;

import com.example.finance.entity.TwoFactorAuth;
import com.example.finance.entity.User;
import com.example.finance.repository.TwoFactorAuthRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import org.apache.commons.codec.binary.Base32;

@Service
public class TwoFactorAuthService {

    @Autowired
    private TwoFactorAuthRepository twoFactorAuthRepository;

    private static final String ISSUER = "FinancialManagement";
    private static final int SECRET_KEY_LENGTH = 20;
    private static final int BACKUP_CODES_COUNT = 10;

    /**
     * Generate secret key for 2FA
     */
    public String generateSecretKey() {
        SecureRandom random = new SecureRandom();
        byte[] bytes = new byte[SECRET_KEY_LENGTH];
        random.nextBytes(bytes);
        Base32 base32 = new Base32();
        return base32.encodeToString(bytes);
    }

    /**
     * Generate backup codes
     */
    public List<String> generateBackupCodes() {
        List<String> codes = new ArrayList<>();
        SecureRandom random = new SecureRandom();
        
        for (int i = 0; i < BACKUP_CODES_COUNT; i++) {
            int code = 100000 + random.nextInt(900000); // 6-digit codes
            codes.add(String.valueOf(code));
        }
        
        return codes;
    }

    /**
     * Setup 2FA for user
     */
    @Transactional
    public Map<String, Object> setupTwoFactorAuth(User user) {
        // Check if 2FA already exists
        Optional<TwoFactorAuth> existing = twoFactorAuthRepository.findByUserId(user.getId());
        
        TwoFactorAuth twoFactorAuth;
        if (existing.isPresent()) {
            twoFactorAuth = existing.get();
        } else {
            twoFactorAuth = new TwoFactorAuth();
            twoFactorAuth.setUser(user);
        }
        
        // Generate new secret key and backup codes
        String secretKey = generateSecretKey();
        List<String> backupCodes = generateBackupCodes();
        
        twoFactorAuth.setSecretKey(secretKey);
        twoFactorAuth.setBackupCodes(String.join(",", backupCodes));
        twoFactorAuth.setEnabled(false); // Not enabled until verified
        
        twoFactorAuthRepository.save(twoFactorAuth);
        
        // Generate QR code URL
        String qrCodeUrl = generateQRCodeUrl(user.getEmail(), secretKey);
        
        Map<String, Object> result = new HashMap<>();
        result.put("secretKey", secretKey);
        result.put("qrCodeUrl", qrCodeUrl);
        result.put("backupCodes", backupCodes);
        
        return result;
    }

    /**
     * Generate QR code URL for authenticator apps
     */
    private String generateQRCodeUrl(String email, String secretKey) {
        return String.format(
            "otpauth://totp/%s:%s?secret=%s&issuer=%s",
            ISSUER,
            email,
            secretKey,
            ISSUER
        );
    }

    /**
     * Generate QR code image
     */
    public byte[] generateQRCodeImage(String qrCodeUrl) throws Exception {
        BitMatrix matrix = new MultiFormatWriter().encode(
            qrCodeUrl,
            BarcodeFormat.QR_CODE,
            300,
            300
        );
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", outputStream);
        
        return outputStream.toByteArray();
    }

    /**
     * Verify TOTP code
     */
    public boolean verifyCode(String secretKey, String code) {
        try {
            long timeWindow = System.currentTimeMillis() / 1000 / 30;
            
            // Check current time window and adjacent windows
            for (int i = -1; i <= 1; i++) {
                String generatedCode = generateTOTP(secretKey, timeWindow + i);
                if (generatedCode.equals(code)) {
                    return true;
                }
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Generate TOTP code
     */
    private String generateTOTP(String secretKey, long timeWindow) throws Exception {
        Base32 base32 = new Base32();
        byte[] bytes = base32.decode(secretKey);
        
        byte[] data = new byte[8];
        long value = timeWindow;
        for (int i = 7; i >= 0; i--) {
            data[i] = (byte) value;
            value >>>= 8;
        }
        
        SecretKeySpec signKey = new SecretKeySpec(bytes, "HmacSHA1");
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(signKey);
        byte[] hash = mac.doFinal(data);
        
        int offset = hash[19] & 0xf;
        long truncatedHash = 0;
        for (int i = 0; i < 4; i++) {
            truncatedHash <<= 8;
            truncatedHash |= (hash[offset + i] & 0xFF);
        }
        
        truncatedHash &= 0x7FFFFFFF;
        truncatedHash %= 1000000;
        
        return String.format("%06d", truncatedHash);
    }

    /**
     * Enable 2FA after verification
     */
    @Transactional
    public boolean enableTwoFactorAuth(User user, String verificationCode) {
        Optional<TwoFactorAuth> twoFactorAuthOpt = twoFactorAuthRepository.findByUserId(user.getId());
        
        if (twoFactorAuthOpt.isEmpty()) {
            return false;
        }
        
        TwoFactorAuth twoFactorAuth = twoFactorAuthOpt.get();
        
        if (verifyCode(twoFactorAuth.getSecretKey(), verificationCode)) {
            twoFactorAuth.setEnabled(true);
            twoFactorAuth.setVerifiedAt(LocalDateTime.now());
            twoFactorAuthRepository.save(twoFactorAuth);
            return true;
        }
        
        return false;
    }

    /**
     * Disable 2FA
     */
    @Transactional
    public boolean disableTwoFactorAuth(User user, String verificationCode) {
        Optional<TwoFactorAuth> twoFactorAuthOpt = twoFactorAuthRepository.findByUserId(user.getId());
        
        if (twoFactorAuthOpt.isEmpty()) {
            return false;
        }
        
        TwoFactorAuth twoFactorAuth = twoFactorAuthOpt.get();
        
        if (verifyCode(twoFactorAuth.getSecretKey(), verificationCode)) {
            twoFactorAuth.setEnabled(false);
            twoFactorAuthRepository.save(twoFactorAuth);
            return true;
        }
        
        return false;
    }

    /**
     * Verify with backup code
     */
    @Transactional
    public boolean verifyBackupCode(User user, String backupCode) {
        Optional<TwoFactorAuth> twoFactorAuthOpt = twoFactorAuthRepository.findByUserId(user.getId());
        
        if (twoFactorAuthOpt.isEmpty()) {
            return false;
        }
        
        TwoFactorAuth twoFactorAuth = twoFactorAuthOpt.get();
        String[] codes = twoFactorAuth.getBackupCodes().split(",");
        List<String> codeList = new ArrayList<>(Arrays.asList(codes));
        
        if (codeList.contains(backupCode)) {
            // Remove used code
            codeList.remove(backupCode);
            twoFactorAuth.setBackupCodes(String.join(",", codeList));
            twoFactorAuthRepository.save(twoFactorAuth);
            return true;
        }
        
        return false;
    }

    /**
     * Check if 2FA is enabled for user
     */
    public boolean isTwoFactorEnabled(User user) {
        Optional<TwoFactorAuth> twoFactorAuth = twoFactorAuthRepository.findByUserIdAndEnabledTrue(user.getId());
        return twoFactorAuth.isPresent();
    }

    /**
     * Get 2FA status
     */
    public Map<String, Object> getTwoFactorStatus(User user) {
        Optional<TwoFactorAuth> twoFactorAuthOpt = twoFactorAuthRepository.findByUserId(user.getId());
        
        Map<String, Object> status = new HashMap<>();
        
        if (twoFactorAuthOpt.isPresent()) {
            TwoFactorAuth twoFactorAuth = twoFactorAuthOpt.get();
            status.put("enabled", twoFactorAuth.getEnabled());
            status.put("verifiedAt", twoFactorAuth.getVerifiedAt());
            
            // Count remaining backup codes
            if (twoFactorAuth.getBackupCodes() != null) {
                int remainingCodes = twoFactorAuth.getBackupCodes().split(",").length;
                status.put("remainingBackupCodes", remainingCodes);
            } else {
                status.put("remainingBackupCodes", 0);
            }
        } else {
            status.put("enabled", false);
            status.put("verifiedAt", null);
            status.put("remainingBackupCodes", 0);
        }
        
        return status;
    }
}
