package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {

    static final String[] REMOTE_PORTS = { "11108" , "11112" , "11116" , "11120" , "11124" };
    static final int SERVER_PORT = 10000;


    // to manage message sequence
    private static int seq = 0;
    private PriorityComparator comparator = new PriorityComparator();
    private PriorityBlockingQueue<Message> queue = new PriorityBlockingQueue(30 , comparator);
    private String myPort = null;
    private static Map<Double , List<Message>> proposalMap = new HashMap<>();

    Map<String , Long> heartBeats = new HashMap<>();
    private static boolean isMessagingStarted = false;
    private static boolean isFailureDetected = false;
    private static String failedPort;
    private static int noOfActiveNodes = 5;

    private static int CPSeq = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));

        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */


        /*
        * This code is taken from PA1
        */
        /*


         * Calculate the port number that this AVD listens on.
         * It is just a hack that I came up with to get around the networking limitations of AVDs.
         * The explanation is provided in the PA1 spec.
         */
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = String.valueOf((Integer.parseInt(portStr) * 2));

        try {
            /*
             * Create a server socket as well as a thread (AsyncTask) that listens on the server
             * port.
             *
             * AsyncTask is a simplified thread construct that Android provides. Please make sure
             * you know how it works by reading
             * http://developer.android.com/reference/android/os/AsyncTask.html
             */
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            /*
             * Log is a good way to debug your code. LogCat prints out all the messages that
             * Log class writes.
             *
             * Please read http://developer.android.com/tools/debugging/debugging-projects.html
             * and http://developer.android.com/tools/debugging/debugging-log.html
             * for more information on debugging.
             */
            Log.e("Activity", "Can't create a ServerSocket");
            return;
        }

        final EditText editText = (EditText)findViewById(R.id.editText1);
        Button send = (Button)findViewById(R.id.button4);

        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String msg = editText.getText().toString() + "\n";
                editText.setText("");

                Log.v("OnClickListener Message" , msg);

                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, myPort);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }




    /***
     * ServerTask is an AsyncTask that should handle incoming messages. It is created by
     * ServerTask.executeOnExecutor() call in SimpleMessengerActivity.
     *
     * Please make sure you understand how AsyncTask works by reading
     * http://developer.android.com/reference/android/os/AsyncTask.html
     *
     * @author stevko
     *
     */
    private class ServerTask extends AsyncTask<ServerSocket, Message, Void> {

        Timer timer = null;
        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];

            /*
             * TODO: Fill in your server code that receives messages and passes them
             * to onProgressUpdate().
             */

            while(true){
                try{

                    ObjectInputStream stream = null;

                    serverSocket.setSoTimeout(2400);
                    stream = new ObjectInputStream(new BufferedInputStream(serverSocket.accept().getInputStream()));

                    Message message = (Message)stream.readObject();

                    if( message.getType().equals("HB") ){
                        heartBeats.put(message.getPortNum() , System.currentTimeMillis());
                        //Log.v("Heartbeats received from : ", message.getPortNum());
                        if( heartBeats.size() >= 4 ){
                            //Timer timer1 = new Timer();
                            //timer1.schedule(new ClearStateClass() , 0);
                            clearFailedNodeState();;
                        }
                    }
                    else{
                        isMessagingStarted = true;
                        updateLocalSequence( message.getFinalSeq());

                        if( !message.isDeliverable() ){

                            if( !message.getPortNum().equals(myPort)){

                                message.setProposalProcessId(createProcessId(myPort));
                                double a = ++seq + (createProcessId(myPort)/10.0);

                                message.setProposedSeq(a);
                                message.setFinalSeq(a);
                                Log.v("Message Added to queue Not Other Port" , message.getMsg() + " , "+ message.isDeliverable() + " , " + message.getFinalSeq());
                                //queue.add(message);
                                addToQueue(message);

                                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                                        Integer.parseInt(message.getPortNum()));

                                ObjectOutputStream oStream = new ObjectOutputStream( new BufferedOutputStream(socket.getOutputStream()));
                                oStream.writeObject(message);
                                oStream.flush();
                                oStream.close();
                                socket.close();
                            }
                            else{

                                double key = Double.parseDouble(message.getSeq()+message.getPortNum());
                                List<Message> list;
                                if( proposalMap.containsKey(key)){
                                    list = proposalMap.get(key);
                                }
                                else{
                                    list = new ArrayList<Message>();
                                }

                                if( message.getProposalProcessId() == 0 ){
                                    //double pSeq = message.getSeq()+(createProcessId(myPort)/10.0);
                                    double pSeq = ++seq+(createProcessId(myPort)/10.0);
                                    message.setProposedSeq(pSeq);
                                    message.setProposalProcessId(createProcessId(myPort));
                                    message.setFinalSeq(pSeq);
                                    Log.v("Message Added to queue Not Same Port" , message.getMsg() + " , "+ message.isDeliverable() + " , " + message.getFinalSeq());
                                    //queue.add(message);
                                    addToQueue(message);
                                }

                                list.add(message);
                                proposalMap.put(key , list );

                                if( list.size() >= noOfActiveNodes ){

                                    double maxId = 0.0;
                                    ListIterator<Message> it = list.listIterator();
                                    while( it.hasNext() ){
                                        Message msg1 = it.next();
                                        double id = msg1.getProposedSeq();
                                        if( id > maxId ){
                                            maxId = id;
                                        }
                                    }
                                    message.setFinalSeq(maxId);
                                    message.setDeliverable(true);
                                    multicast(message);
                                    proposalMap.remove(key);

                                }
                            }
                        }
                        else{
                            editQueue( message );

                            while(!queue.isEmpty()){
                                if( isFailureDetected ){
                                    //Log.v("Inside isFailureDetected true" , "failed Port : "+failedPort);
                                    if(queue.peek().isDeliverable()){
                                        publishProgress(queue.poll());
                                    }
                                    else if( queue.peek().getPortNum().equalsIgnoreCase(failedPort)){
                                        queue.poll();
                                    }
                                    else
                                        break;
                                }
                                else{
                                    if( queue.peek().isDeliverable() )
                                        publishProgress(queue.poll());
                                    else
                                        break;
                                }

                            }

                        }
                    }

                }
                catch (ClassNotFoundException e){
                    Log.e("Server" , "Message class not found");
                }
                catch (SocketException e){
                    Log.e("Server" , "Socket Exception");
                    //e.printStackTrace();
                }
                catch (SocketTimeoutException e){
                    if( CPSeq >= 24 )
                        isMessagingStarted = false;
                    if(  queue.size() > 0 && !isFailureDetected /*CPSeq >= 0 && !isFailureDetected*/ ){
                        Log.e("Server" , "Socket timeout");
                        isMessagingStarted = false;
                        isFailureDetected = true;

                        sendHeartBeats();
                    }
                }
                catch(IOException e){
                    Log.e("Server", "Error in reading message");
                }
                catch (Exception e){
                    Log.e("Server" , "Exception");
                    e.printStackTrace();
                }
            }

        }

        protected void onProgressUpdate(Message...msgs) {
            /*
             * The following code displays what is received in doInBackground().
             */
            Message msg = msgs[0];
            TextView tv = (TextView) findViewById(R.id.textView1);
            tv.append(msg.getMsg());

            String messageRec = msg.getMsg();

            // Setting the value in ContentProvider [Used from PA2 specification]

            CPSeq++;

            ContentValues keyValueToInsert = new ContentValues();
            Log.v("Msg entry to content provider",messageRec+" , "+CPSeq);
            keyValueToInsert.put("key",CPSeq);
            keyValueToInsert.put("value",messageRec);

            Uri newUri = getContentResolver().insert(
                    GroupMessengerProvider.providerUri,
                    keyValueToInsert
            );


            return;
        }
    }


    /***
     * ClientTask is an AsyncTask that should send a string over the network.
     * It is created by ClientTask.executeOnExecutor() call whenever OnKeyListener.onKey() detects
     * an enter key press event.
     *
     * @author stevko
     *
     */
    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            //try {

            Socket socket = null;
            ObjectOutputStream stream = null;
                /*
                * Looping to send the message to all the AVD's. There is no need to identify remote ports
                * because we have to send it to all AVD's including the client.
                */

            String msgToSend = msgs[0];
            seq++;
            Log.v("Client Msg",msgToSend +" from "+myPort);
            Message message = new Message(seq , createProcessId(msgs[1]) , msgs[1] , false);
            message.setMsg(msgToSend);
            multicast(message);

            return null;
        }
    }


    public void multicast(Message message){
        try {

            Socket socket = null;
            ObjectOutputStream stream = null;
                /*
                * Looping to send the message to all the AVD's. There is no need to identify remote ports
                * because we have to send it to all AVD's including the client.
                */
            for(int counter = 0; counter < 5; counter++){
                socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(REMOTE_PORTS[counter]));

                stream = new ObjectOutputStream( new BufferedOutputStream(socket.getOutputStream()));
                stream.writeObject(message);
                stream.flush();
                stream.close();
                socket.close();

            }

        } catch (UnknownHostException e) {
            Log.e("Client", "ClientTask UnknownHostException");
        }
        catch (SocketTimeoutException e) {
            Log.e("Client", "ClientTask socket Timeout");
        }
        catch (IOException e) {
            Log.e("Client", "ClientTask socket IOException");
        }

    }


    public void printMessage(Message message){
        /*Log.v("******Message******" ,message.getMsg());
        Log.v("******Message Port******" ,message.getPortNum());
        Log.v("******seq******" ,message.getSeq()+"");
        Log.v("******Sender******" ,message.getSenderProcessId()+"");
        Log.v("******Proposed Sender******" ,message.getProposalProcessId()+"");
        Log.v("******Proposed Seq******" ,message.getProposedSeq()+"");
        Log.v("******final seq******" ,message.getFinalSeq()+"");
        Log.v("******Message******" ,message.getMsg());*/
    }


    public int createProcessId( String portNum){
        int processId = 0;
        switch (portNum){
            case "11108":
                processId = 1;
                break;
            case "11112":
                processId = 2;
                break;
            case "11116":
                processId = 3;
                break;
            case "11120":
                processId = 4;
                break;
            case "11124":
                processId = 5;
        }

        return processId;
    }


    public void editQueue(Message msg){
        Queue<Message> tempQ = new LinkedList<>();
        tempQ.addAll(queue);
        Message tobeRemoved = null;

        Iterator<Message> it = tempQ.iterator();
        while (it.hasNext()){
            Message temp = it.next();

            if( temp.getPortNum().equalsIgnoreCase(msg.getPortNum()) && temp.getSeq() == msg.getSeq() && temp.getSenderProcessId() == msg.getSenderProcessId() ){
                Log.v("GOT THE MESSAGE" , "**Port "+temp.getPortNum()+","+msg.getPortNum()+"  **Seq "+temp.getSeq()+" , "+msg.getSeq()+"  ** SenderProcess "+temp.getSenderProcessId()+","+msg.getSenderProcessId());
                tobeRemoved = temp;
                break;
            }
        }

        boolean fl = queue.remove(tobeRemoved);
        Log.v("msg removed from queue" , fl+"");
        Log.v("Message Added to queue editQueue()" , msg.getMsg() + " , "+ msg.isDeliverable() + " , " + msg.getFinalSeq());
        //queue.add(msg);
        if( fl )
            addToQueue(msg);

    }

    public void updateLocalSequence(double maxObserved){
        if( maxObserved > seq ){
            seq = (int)Math.ceil(maxObserved) + 1;
        }
    }

    public void sendHeartBeats(){
        Message msg = new Message(myPort);
        multicast(msg);
    }


    public synchronized void addToQueue(Message msg){
        queue.add(msg);
    }



    public void clearFailedNodeState(){
        for(String port : REMOTE_PORTS){
            Log.v("size of heartBeats" , ""+heartBeats.size());
            if( !port.equals(myPort) && !heartBeats.containsKey(port) ){
                failedPort = port;
                Log.v("TimerTask" , "failed Port detected : "+failedPort);
                noOfActiveNodes = 4;
                break;
            }
        }



        Log.v("TimerTask" , "clearState() started");
        Queue<Message> tempQ = new LinkedList<>();
        tempQ.addAll(queue);
        Message tobeRemoved = null;

        Iterator<Message> it = tempQ.iterator();
        while (it.hasNext()){
            Message temp = it.next();

            if( !temp.isDeliverable() && temp.getPortNum().equalsIgnoreCase(failedPort)){
                Log.v("GOT THE MESSAGE" , temp.getMsg());
                tobeRemoved = temp;

                queue.remove(tobeRemoved);
            }
        }

        //clear();
        //noOfActiveNodes = 4;
        Log.v("TimerTask" , "clearState() ends");





        Log.v("TimerTask" , "clear() started");
        Iterator mapIt = proposalMap.keySet().iterator();
        while(mapIt.hasNext() ){
            double key = (Double)mapIt.next();
            List<Message> list = proposalMap.get(key);

            if( list.size() == 4 ){
                double maxId = 0.0;
                ListIterator<Message> it1 = list.listIterator();
                Message msg1 = null;
                boolean flag = true;
                while( it1.hasNext() ){
                    msg1 = it1.next();
                    double id = msg1.getProposedSeq();

                    if( msg1.getPortNum().equalsIgnoreCase(failedPort) ){
                        flag = false;
                        break;
                    }


                    if( id > maxId ){
                        maxId = id;
                    }
                }

                Log.v("Message with 4 replies",msg1.getMsg()+" , sender:"+msg1.getSeq());

                if( flag ){
                    msg1.setFinalSeq(maxId);
                    msg1.setDeliverable(true);
                    multicast(msg1);

                    mapIt.remove();
                    Log.v("Is REMOVED??",proposalMap.containsKey(key)+"");
                }

            }
        }

        noOfActiveNodes = 4;
        Log.v("TimerTask" , "clear() finished");

    }


    /*class ClearStateClass extends TimerTask{
        @Override
        public void run() {
            Log.v("TimerTask" , "run()");
            detectFailedNode();
        }

        public void detectFailedNode(){
            for(String port : REMOTE_PORTS){
                Log.v("size of heartBeats" , ""+heartBeats.size());
                if( !port.equals(myPort) && !heartBeats.containsKey(port) ){
                    failedPort = port;
                    Log.v("TimerTask" , "failed Port detected : "+failedPort);
                    noOfActiveNodes = 4;
                    break;
                }
            }
            clearState();
            //clear();
            Log.v("TimerTask" , "end detectFailedNode()");
        }



        *//*public void clear(){
            Log.v("TimerTask" , "clear() started");
            Iterator mapIt = proposalMap.keySet().iterator();
            while(mapIt.hasNext() ){
                double key = (Double)mapIt.next();

                if( String.valueOf(key).contains(failedPort) ){
                    mapIt.remove();
                    continue;
                }

                List<Message> list = proposalMap.get(key);

                if( list.size() == 4 ){
                    double maxId = 0.0;
                    ListIterator<Message> it = list.listIterator();
                    Message msg1 = null;
                    boolean flag = true;
                    while( it.hasNext() ){
                        msg1 = it.next();
                        double id = msg1.getProposedSeq();

                        if( msg1.getPortNum().equalsIgnoreCase(failedPort) ){
                            flag = false;
                            break;
                        }


                        if( id > maxId ){
                            maxId = id;
                        }
                    }

                    Log.v("Message with 4 replies",msg1.getMsg()+" , sender:"+msg1.getSeq());

                    if( flag ){
                        msg1.setFinalSeq(maxId);
                        msg1.setDeliverable(true);
                        multicast(msg1);

                        mapIt.remove();
                        Log.v("Is REMOVED??",proposalMap.containsKey(key)+"");
                    }

                }
            }

            noOfActiveNodes = 4;
            Log.v("TimerTask" , "clear() finished");

        }*//*


        public void clear(){
            Log.v("TimerTask" , "clear() started");
            Iterator mapIt = proposalMap.keySet().iterator();
            while(mapIt.hasNext() ){
                double key = (Double)mapIt.next();
                List<Message> list = proposalMap.get(key);

                if( list.size() == 4 ){
                    double maxId = 0.0;
                    ListIterator<Message> it = list.listIterator();
                    Message msg1 = null;
                    boolean flag = true;
                    while( it.hasNext() ){
                        msg1 = it.next();
                        double id = msg1.getProposedSeq();

                        if( msg1.getPortNum().equalsIgnoreCase(failedPort) ){
                            flag = false;
                            break;
                        }


                        if( id > maxId ){
                            maxId = id;
                        }
                    }

                    Log.v("Message with 4 replies",msg1.getMsg()+" , sender:"+msg1.getSeq());

                    if( flag ){
                        msg1.setFinalSeq(maxId);
                        msg1.setDeliverable(true);
                        multicast(msg1);

                        mapIt.remove();
                        Log.v("Is REMOVED??",proposalMap.containsKey(key)+"");
                    }

                }
            }

            noOfActiveNodes = 4;
            Log.v("TimerTask" , "clear() finished");

        }


        public void clearState(){
            Log.v("TimerTask" , "clearState() started");
            Queue<Message> tempQ = new LinkedList<>();
            tempQ.addAll(queue);
            Message tobeRemoved = null;

            Iterator<Message> it = tempQ.iterator();
            while (it.hasNext()){
                Message temp = it.next();

                if( !temp.isDeliverable() && temp.getPortNum().equalsIgnoreCase(failedPort)){
                    Log.v("GOT THE MESSAGE" , temp.getMsg());
                    tobeRemoved = temp;

                    queue.remove(tobeRemoved);
                }
            }

            clear();
            //noOfActiveNodes = 4;
            Log.v("TimerTask" , "clearState() ends");
        }
    }*/

}


