import java.io.IOException;
import java.net.*;
import java.util.Calendar;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Server{
    private  DatagramSocket socket;  //实现UDP编码的工具包
    private  DatagramPacket packet;  //封装数据报的类
    private final int port = 1111;
    private String ack="ack";
    private int seq=1;
    ConcurrentHashMap<Integer,Message> receivebuffer=new ConcurrentHashMap<>();
    private boolean receivemessage=false;


    final ExecutorService exec = Executors.newFixedThreadPool(1);
    Callable<String> call = new Callable<String>(){
        @Override
        public String call() throws Exception{
            socket.receive(packet);
            return "成功";
        }
    };
    public Server() {
        try {
            socket = new DatagramSocket(port);  //server 进程和该端口号绑定
            byte[] bytes = new byte[1024];
            packet = new DatagramPacket(bytes, 0, bytes.length);
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    public int timer(){
        Calendar c= Calendar.getInstance();
        int year=c.get(Calendar.YEAR);
        int minute=c.get(Calendar.MINUTE);
        int second=c.get(Calendar.SECOND);
        int time=year*3600+minute*60+second;
//        System.out.println(time);
        return time;
    }

    private boolean whetherack() throws InterruptedException, TimeoutException {
        String obj = null;
        boolean result=false;
        try {
            Future<String> future = exec.submit(call);
            obj=future.get(2000, TimeUnit.MILLISECONDS);
            String expectack = new String(packet.getData(), 0, packet.getLength());
            if (expectack.equals("ack")) {
                System.out.println("成功接收到客服端ack消息");
                result=true;
            }
        } catch (ExecutionException e) {
            System.out.println("未收到客户端ack");
        }
        return result;
    }

    public void tcpconnect() throws IOException, InterruptedException, TimeoutException {
        System.out.println("等待客户端连接");
        socket.receive(packet);
        String expectsyn = new String(packet.getData(), 0, packet.getLength());
        if (expectsyn.equals("syn")){
            System.out.println("收到"+packet.getPort()+"端口客户端传来的syn消息");
            byte[] bytes = ack.getBytes();
            DatagramPacket ackmessage = new DatagramPacket(bytes, 0, bytes.length,
                packet.getAddress(), packet.getPort());
            socket.send(ackmessage);
            System.out.println("向"+packet.getPort()+"端口客户端回复ack消息");
            if(whetherack()==true)
            {
                System.out.println("收到"+packet.getPort()+"ack消息");
                receivemessage=true;
            }
        }
    }

    public void startreceivemessage() {
        try {
            System.out.println("与端口号为"+packet.getPort()+"建立连接");
            while(receivemessage) {
                socket.receive(packet);
                String str = new String(packet.getData(), 0, packet.getLength());
                if(str.equals("fin"))
                {
                    receivemessage=false;
                    break;
                }
                Message msg=new Message(null,0,0,0,null);
                Message.decoder(str,msg);
                if(seq!=msg.getSeq()&&msg.getType().equals("psh"))
                {
                    receivebuffer.put(msg.getSeq()*100+seq,msg);
                    System.out.println("1");
                    Message ACK=new Message(ack,msg.getSeq(),timer(),msg.getSendtime(),msg.getMessagecontent());
                    byte[] bytes= Message.encoder(ACK).getBytes();
                    DatagramPacket ackpacket = new DatagramPacket(bytes, 0, bytes.length,
                            packet.getAddress(), packet.getPort());
//                System.out.println(packet.getAddress());
                    socket.send(ackpacket);
                }
                AtomicInteger nowseq= new AtomicInteger();
                if(seq==msg.getSeq()&&msg.getType().equals("psh"))
                {
                    System.out.println("接收到客户端发送的数据为： " + msg.getMessagecontent());
                    if(!receivebuffer.isEmpty())
                    {
                        receivebuffer.forEach((key, tempmessage) ->
                        {
                            if (key%100==seq) {
                                System.out.println("接收到客户端发送的数据为： " + tempmessage.getMessagecontent());
                                receivebuffer.remove(key,tempmessage);
                                nowseq.set(tempmessage.getSeq());
                            }
                        });
                    }
                    Message ACK=new Message(ack,msg.getSeq(),timer(),msg.getSendtime(),msg.getMessagecontent());
                    byte[] bytes= Message.encoder(ACK).getBytes();
                    DatagramPacket ackpacket = new DatagramPacket(bytes, 0, bytes.length,
                        packet.getAddress(), packet.getPort());
//                System.out.println(packet.getAddress());
                socket.send(ackpacket);
//                System.out.println("1");
                    seq++;
//                    System.out.println(nowseq.get());
                    if(nowseq.get()>=seq){
                        seq= nowseq.get()+1;
                    }
                }
            }
        }catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            socket.close();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException, TimeoutException {
        Server server=new Server();
        server.tcpconnect();
        server.startreceivemessage();
        System.exit(0);
    }
}
