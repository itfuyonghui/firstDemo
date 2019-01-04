package com.ultrapower.porotocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import ch.ethz.ssh2.ChannelCondition;
import ch.ethz.ssh2.Connection;
import ch.ethz.ssh2.InteractiveCallback;
import ch.ethz.ssh2.Session;

import com.ultrapower.driver.resdriver.keyword.Colon;
import com.ultrapower.driver.resdriver.transport.Transport;
import com.ultrapower.driver.resdriver.transport.TransportAuthenticationException;
import com.ultrapower.driver.resdriver.transport.TransportException;
import com.ultrapower.driver.resdriver.transport.VirtualTerminal;

/**
 * <p>Title: </p>
 *
 * <p>Description: </p>
 *
 * <p>Copyright: Copyright (c) 2007</p>
 *
 * <p>Company: </p>
 *
 * @author not attributable
 * @version 1.0
 */
public class SSHProtocol extends Transport {

    private static final int DEFAULT_PORT = 22;

    private boolean authed = false;

    private String prompt;

    private Connection conn = null;

    private Session session = null;

    public DataInputStream dis = null;

    public DataOutputStream dos = null;

    // 初始化buffer
    private byte[] buffer = new byte[1024];

    private VirtualTerminal virtualTerminal = new VirtualTerminal();

    /**
     * 资源回显编码
     */
    private String sysEncoding;

    public String getSysEncoding() {
        return sysEncoding;
    }

    public void setSysEncoding(String sysEncoding) {
        this.sysEncoding = sysEncoding;
    }

    public SSHProtocol() {
        super();
    }

    public void ini(String ip, int port, int timeout, int tryTimes,
                    String echoTime) throws TransportException {

        this.setIp(ip);
        this.setPort(port);
        this.setTimeout(timeout);
        this.setTryTimes(tryTimes);
        if( (null!=echoTime) && !("".equals(echoTime))){
            this.setEchoTime(new Integer(echoTime));
        }

        this.conn = new Connection(ip, port);
        try {
            //this.conn.connect();
            this.conn.connect(null, getTimeout(), getTimeout());
            this.setIp(ip);
        } catch (IOException ex) {
            throw new TransportException(NET_ERROR, ex);
        }
    }

    /**
     * authentication
     *
     * @param uid String
     * @param pwd String
     * @throws
     * @throws TransportException
     * @todo Implement this ams.resdriver.transport.Transport method
     */
    public void auth(String uid, String pwd, String mainPrompt)
            throws TransportAuthenticationException, TransportException {
        this.ioTest();
        try {

            System.out.println("---------1---------");
            if(conn.isAuthMethodAvailable(uid, "password")){//支持password authentication认证
                this.setAuthed(this.conn.authenticateWithPassword(uid, pwd));
                /**
                 * 0021654: 广东移动三网统建4A项目-资源从帐号管理-获取同步帐号问题
                 * add  by qujiangbo 2012-08-21
                 * */
                if(false == this.isAuthed()
                        && conn.isAuthMethodAvailable(uid, "keyboard-interactive")){
                    this.setAuthed(this.conn.authenticateWithKeyboardInteractive(uid, new InteractiveLogic(pwd)));
                }
            }else if(conn.isAuthMethodAvailable(uid, "keyboard-interactive")){//支持keyboard-interactive authentication认证
                this.setAuthed(this.conn.authenticateWithKeyboardInteractive(uid, new InteractiveLogic(pwd)));
            }else{
                appendEcho("不支持的认证方式");
                throw new TransportAuthenticationException("不支持的认证方式");
            }

            System.out.println("---------2---------： " + this.isAuthed());

            if (this.isAuthed()) {
                System.out.println("---------认证成功---------" );
                this.session = this.conn.openSession();
                this.session.requestPTY("vt100", 80, 800, 0, 0, null);
                this.session.startShell();
                this.dis = new DataInputStream(this.session.getStdout());
                this.dos = new DataOutputStream(this.session.getStdin());
                this.sendCommand("");
                String echo = this.read2KeyWordEcho(mainPrompt);
            } else {
                System.out.println("---------认证失败---------");
                appendEcho("用户名或密码不正确，认证失败！");
                throw new TransportAuthenticationException("uid[" + uid
                        + "]->ssh[" + this.getIp() + "]:[" + this.getPort()
                        + "] 认证失败");
            }
        } catch (IOException ex) {
            appendEcho("网络连接失败，请检查IP或端口是否正确！");
            throw new TransportException(NET_ERROR, ex);
        }
    }

    /**
     * close
     *
     * @throws TransportException
     * @todo Implement this ams.resdriver.transport.Transport method
     */
    public void close() throws TransportException {

        try {
            if (this.dis != null) {
                this.dis.close();
                this.dis = null;
            }
            if (this.dos != null) {
                this.dos.close();
                this.dos = null;
            }

            if (this.session != null) {
                this.session.close();
                this.session = null;
            }

            if (this.conn != null) {
                this.conn.close();
                this.conn = null;
            }

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * readUntilColon
     *
     * @return String
     * @throws TransportException
     * @throws TransportException
     * @todo Implement this ams.resdriver.transport.Transport method
     */
    public String read2ColonEcho() throws TransportException {
        StringBuffer inputStr = new StringBuffer();

        do {
            inputStr.append(this.readEcho().trim());
        } while (!Colon.endsWith(inputStr.toString()));

        return inputStr.toString();
    }

    /**
     * readUntilKeyWord
     *
     * @param keyWord String
     * @return String
     * @throws TransportException
     * @todo Implement this ams.resdriver.transport.Transport method
     */
    @Deprecated
    public String read2KeyWordEcho(String keyWord) throws TransportException {
        String rv = "";
        long startTime = System.currentTimeMillis();

        do {
            rv += this.readEcho();

            if(rv.indexOf("Illegal password")>-1){
                throw new TransportException(rv, new Exception());
            }

            if((System.currentTimeMillis() - startTime) >= this.getTimeout()){
                appendEcho("read time out: " + keyWord);
                throw new TransportException("read time out: " + keyWord);
            }else{
                this.sendCommand(" ");
            }
        } while (!rv.trim().endsWith(keyWord));
        return rv;
    }

    /**
     * recieveEcho
     *
     * @return String
     * @throws TransportException
     * @todo Implement this ams.resdriver.transport.Transport method
     */
    public String readEcho() throws TransportException {
        this.sessionTest();
        ArrayList bsList = new ArrayList();
        try {
            int ival = this.session.waitForCondition(ChannelCondition.STDOUT_DATA, getTimeout());

            if(ival == 1){
                throw new TransportException("读取回显超时，请把资源回显时间设置大一些！");
            }

            int len = dis.read(this.buffer);
            for (int i = 0; i < len; i++) {
                char c = toChar(this.buffer[i]);
                Byte b = virtualTerminal.process(c);
                if (b != null) {
                    bsList.add(b);
                }
                continue;
            }
            int size = bsList.size();
            byte[] bs = new byte[size];
            for (int i = 0; i < size; i++) {
                bs[i] = ((Byte) bsList.get(i)).byteValue();
            }

            String result = new String(bs, this.getEncode());
            appendEcho(result);

            return result;
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
            throw new TransportException("回显编码转换" + this.getEncode() + "异常");
        } catch (IOException ex) {
            ex.printStackTrace();
            throw new TransportException(NET_ERROR, ex);
        }

    }

    /**
     * sendCmd
     *
     * @param cmd String
     * @throws TransportException
     * @todo Implement this ams.resdriver.transport.Transport method
     */
    public void sendCommand(String cmd) throws TransportException {
        this.sessionTest();

        if (cmd == null) {
            throw new TransportException("送出的命令为空");
        }

        try {
            this.dos.write(cmd.getBytes());
            this.dos.write(this.COMMAND_END.getBytes());
            try {
                Thread.sleep(this.getEchoTime());
            } catch (InterruptedException ex1) {
            }
        } catch (IOException ex) {
            throw new TransportException("发送命令异常", ex);
        }

    }

    private void ioTest() throws TransportException {
        if (this.conn == null) {
            throw new TransportException(NET_ERROR);
        }

    }

    private void sessionTest() throws TransportException {
        if (this.session == null || this.dis == null || this.dos == null) {
            throw new TransportException("会话未建立");
        }
    }

    /**
     *
     * @param b byte
     * @return char
     */
    private char toChar(byte b) {
        return (char) (b & 0xff);
    }

    public void login(String uid, String pwd, String prompt, int tryTime)
            throws TransportException {
        this.auth(uid, pwd, prompt);
    }

    public String getEncode() {
        // TODO Auto-generated method stub
        return DEFAULT_ENCODE;
    }

    public boolean isAuthed() {
        return authed;
    }

    public void setAuthed(boolean authed) {
        this.authed = authed;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public void sendJPCommand(String cmd) throws TransportException {
        ioTest();
        if (cmd == null) {
            throw new TransportException("送出的命令为空");
        }
        try {
            this.dos.write(cmd.getBytes());
//            this.dos.writeChar(32);
            this.dos.write(this.COMMAND_END.getBytes());
//            this.dos.flush();
            try {
                Thread.sleep(this.getEchoTime());
            } catch (InterruptedException ex1) {
            }
        } catch (IOException ex) {
            throw new TransportException("发送命令异常", ex);
        }
    }

    public void authNoneUsername(String uid, String pwd, String prompt)
            throws TransportAuthenticationException, TransportException {
        // TODO Auto-generated method stub

    }


    public void authJuniper(String userid, String passwd, String prompt)
            throws TransportAuthenticationException, TransportException {

        this.setPrompt(prompt);
        this.ioTest();
        this.sendAuthData4JP(userid, passwd);
    }

    private void sendAuthData4JP(String userid, String passwd)
            throws TransportException {
        String inputStr = read2ColonEcho();
        this.sendJPCommand(userid);
        // inputStr = read2ColonEcho();
        this.sendJPCommand(passwd);
        // this.sendCommand("");
    }

    public void sendHWComment(String cmd) throws TransportException {

        // TODO Auto-generated method stub
        if (cmd == null) {
            throw new TransportException("送出的命令为空");
        }
        try {
            this.dos.write(cmd.getBytes());
//	            this.dos.write(this.COMMAND_END.getBytes());
            try {
                Thread.sleep(this.getEchoTime());
            } catch (InterruptedException ex1) {
            }
        } catch (IOException ex) {
            throw new TransportException("发送命令异常", ex);
        }
    }

}

class InteractiveLogic implements InteractiveCallback{

    String pwd;

    public InteractiveLogic(String pwd){
        this.pwd = pwd;
    }

    public String[] replyToChallenge(String name, String instruction, int numPrompts, String[] prompt, boolean[] echo) throws Exception {
        // TODO Auto-generated method stub
        String[] result = new String[numPrompts];

        for(int i = 0; i < numPrompts; i++){
            result[i] = pwd;
        }

        return result;
    }

}
