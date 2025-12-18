package com.ibsecurity.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Утилиты для работы с паролями.
 * Использует BCrypt для безопасного хеширования.
 */
public class PasswordUtils {
    
    private static final int BCRYPT_ROUNDS = 12;
    
    /**
     * Хеширует пароль.
     */
    public static String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(BCRYPT_ROUNDS));
    }
    
    /**
     * Проверяет пароль.
     */
    public static boolean verifyPassword(String password, String storedHash) {
        if (password == null || storedHash == null) {
            return false;
        }
        try {
            return BCrypt.checkpw(password, storedHash);
        } catch (Exception e) {
            return false;
        }
    }
}
