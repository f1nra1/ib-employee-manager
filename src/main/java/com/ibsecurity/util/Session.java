package com.ibsecurity.util;

import com.ibsecurity.model.User;

/**
 * Управление сессией пользователя.
 */
public class Session {
    
    private static User currentUser = null;
    
    /**
     * Создание сессии при успешной аутентификации.
     */
    public static void createSession(User user) {
        currentUser = user;
        if (currentUser != null) {
            currentUser.setPasswordHash(null); // Не храним пароль в памяти
        }
    }
    
    /**
     * Установка текущего пользователя.
     */
    public static void setCurrentUser(User user) {
        createSession(user);
    }
    
    /**
     * Получение текущего пользователя.
     */
    public static User getCurrentUser() {
        return currentUser;
    }
    
    /**
     * Проверка авторизации.
     */
    public static boolean isLoggedIn() {
        return currentUser != null;
    }
    
    /**
     * Проверка прав администратора.
     */
    public static boolean isAdmin() {
        return currentUser != null && currentUser.isAdmin();
    }
    
    /**
     * Завершение сессии.
     */
    public static void logout() {
        currentUser = null;
    }
}
