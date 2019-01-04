package com.ultrapower.controller;

import com.ultrapower.driver.exception.ResDriverException;
import com.ultrapower.driver.resdriver.common.arg.OpenArg;
import com.ultrapower.driver.resdriver.common.arg.UserArg;
import com.ultrapower.driver.resdriver.gd.netunit.NetUnit;
import com.ultrapower.driver.resdriver.netunit.arg.NetUnitDriverArg;
import com.ultrapower.driver.resdriver.transport.TelnetProtocol;
import com.ultrapower.driver.resdriver.transport.TransportAuthenticationException;
import com.ultrapower.driver.resdriver.transport.TransportException;
import com.ultrapower.pojo.CvsObject;
import com.ultrapower.pojo.LinkResult;

import java.util.List;

/**
 * Created by dell on 2018/12/27.
 */
public class CvsTelnet {

    /**
     * 使用Telnet连接驱动
     * @param arg
     */
    public LinkResult open(CvsObject arg)  {
        TelnetProtocol telnetProtocol = new TelnetProtocol();
        try {
            telnetProtocol.ini(arg.getIp(), Integer.parseInt(arg.getPort()), 30, 30, "3");
        } catch (TransportException e) {
            e.printStackTrace();
            System.out.println("连接失败");
            return new LinkResult(false,"连接失败");
        }
        try {
            telnetProtocol.auth(arg.getAccount(), arg.getPassWord(), arg.getPrompt());
            return new LinkResult(true,"telnet驱动登录成功");
        } catch (TransportException e) {
            e.printStackTrace();
            System.out.println("登录失败");
            return new LinkResult(false,"登录失败");
        }


    }


}
