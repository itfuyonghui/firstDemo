package com.ultrapower.pojo;

/**
 * Created by dell on 2018/12/27.
 */
public class CvsObject {
    private String ip;
    private String port;
    private String protocol;
    private String account;
    private String passWord;
    private String Prompt;
    private String suCmd;
    private String superPassWord;
    private String superPrompt;
    private int timeOut;
    private int tryTimes;
    private String echoTime;
    private String connResult;
    private String loginResult;
    private String suResult;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getPassWord() {
        return passWord;
    }

    public void setPassWord(String passWord) {
        this.passWord = passWord;
    }

    public String getSuCmd() {
        return suCmd;
    }

    public void setSuCmd(String suCmd) {
        this.suCmd = suCmd;
    }

    public String getSuperPassWord() {
        return superPassWord;
    }

    public void setSuperPassWord(String superPassWord) {
        this.superPassWord = superPassWord;
    }

    public String getPrompt() {
        return Prompt;
    }

    public void setPrompt(String prompt) {
        Prompt = prompt;
    }

    public String getSuperPrompt() {
        return superPrompt;
    }

    public void setSuperPrompt(String superPrompt) {
        this.superPrompt = superPrompt;
    }

    public int getTimeOut() {
        return timeOut;
    }

    public void setTimeOut(int timeOut) {
        this.timeOut = timeOut;
    }

    public int getTryTimes() {
        return tryTimes;
    }

    public void setTryTimes(int tryTimes) {
        this.tryTimes = tryTimes;
    }

    public String getEchoTime() {
        return echoTime;
    }

    public void setEchoTime(String echoTime) {
        this.echoTime = echoTime;
    }

    public String getConnResult() {
        return connResult;
    }

    public void setConnResult(String connResult) {
        this.connResult = connResult;
    }

    public String getLoginResult() {
        return loginResult;
    }

    public void setLoginResult(String loginResult) {
        this.loginResult = loginResult;
    }

    public String getSuResult() {
        return suResult;
    }

    public void setSuResult(String suResult) {
        this.suResult = suResult;
    }
}
