package com.ace.taskapi;

public class AuthResponse {

    private String token;
    private Long userId;
    private String username;
    private Integer totalWins;
    private Integer totalLosses;
    private String message;

    public AuthResponse(String token, Long userId, String username,
                        Integer totalWins, Integer totalLosses, String message) {
        this.token       = token;
        this.userId      = userId;
        this.username    = username;
        this.totalWins   = totalWins;
        this.totalLosses = totalLosses;
        this.message     = message;
    }

    public String  getToken()       { return token; }
    public Long    getUserId()      { return userId; }
    public String  getUsername()    { return username; }
    public Integer getTotalWins()   { return totalWins; }
    public Integer getTotalLosses() { return totalLosses; }
    public String  getMessage()     { return message; }
}