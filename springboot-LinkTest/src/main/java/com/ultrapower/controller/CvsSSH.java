package com.ultrapower.controller;

import com.ultrapower.driver.resdriver.transport.SSHProtocol;
import com.ultrapower.driver.resdriver.transport.TransportException;
import com.ultrapower.pojo.CvsObject;
import com.ultrapower.pojo.LinkResult;

/**
 * Created by dell on 2018/12/27.
 */
public class CvsSSH{
    /**
     * 使用Telnet连接驱动
     * @param arg
     */
    public LinkResult open(CvsObject arg) {
        SSHProtocol sshProtocol = new SSHProtocol();
        arg.setTimeOut(30);
        arg.setTryTimes(3);
        arg.setEchoTime("30");
        try {
            sshProtocol.ini(arg.getIp(), Integer.parseInt(arg.getPort()), arg.getTimeOut(), arg.getTryTimes(), arg.getEchoTime());
        } catch (TransportException e) {
            e.printStackTrace();
            System.out.println("连接失败");
            return new LinkResult(false,"连接失败");
        }
        try {
            sshProtocol.auth(arg.getAccount(), arg.getPassWord(), arg.getPrompt());
            return new LinkResult(true,"SSH驱动登录成功");
        } catch (TransportException e) {
            e.printStackTrace();
            System.out.println("登录失败");
            return new LinkResult(false,"登录失败");
        }
    }

    public static void main(String[] args) {
        CvsObject cvsObject = new CvsObject();
        cvsObject.setAccount("root" );
        cvsObject.setIp("10.251.111.244");
        cvsObject.setPassWord("root1234");
        cvsObject.setEchoTime("30");
        cvsObject.setTimeOut(30);
        cvsObject.setTryTimes(3);
        cvsObject.setPort("22");
        cvsObject.setPrompt("#");
        CvsSSH ssh = new CvsSSH();
        ssh.open(cvsObject);
    }


}
