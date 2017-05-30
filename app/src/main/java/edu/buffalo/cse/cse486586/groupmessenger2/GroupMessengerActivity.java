package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.PriorityQueue;

import static android.content.ContentValues.TAG;
import static edu.buffalo.cse.cse486586.groupmessenger2.FeedReaderDbHelper.KEY_FIELD;
import static edu.buffalo.cse.cse486586.groupmessenger2.FeedReaderDbHelper.VALUE_FIELD;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    static final int SERVER_PORT = 10000;
    float MAXAgreedPriority = 0;
    float MAXlocalPriority = 0;


    int seq = -1;


    private Uri buildUri(String scheme, String authority) {

        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

    public float getMaxPriority(PriorityQueue<Message> m) {
//        Iterator<Message> it = m.iterator();
//        float maxPriority = 0;
//        while(it.hasNext()) {
//            Message m1 = it.next();
//            if(m1.pr > maxPriority)
//                maxPriority = m1.pr;
//        }

        return Math.max(MAXAgreedPriority,MAXlocalPriority);
    }

    Uri mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger2.provider");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        if(myPort.equals(REMOTE_PORT0)){
            GlobalValue.ProcessId=0;
        }
        else if(myPort.equals(REMOTE_PORT1)){
            GlobalValue.ProcessId=1;
        }
        else if(myPort.equals(REMOTE_PORT2)){
            GlobalValue.ProcessId=2;
        }
        else if(myPort.equals(REMOTE_PORT3)){
            GlobalValue.ProcessId=3;
        }
        else{
            GlobalValue.ProcessId=4;
        }
        try {

            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {

            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }

        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());


        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        final Button button = (Button) findViewById(R.id.button4);
        button.setOnClickListener(new View.OnClickListener() {


            @Override
            public void onClick(View v) {
                final EditText editText = (EditText) findViewById(R.id.editText1);
                String msg = editText.getText().toString() + "\n";

                editText.setText("");
                Log.d("Goingtoclient",msg);

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
    PriorityQueue<Message> p1 = new PriorityQueue<Message>();
    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {
        // int initial =0;



        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];


            Socket newsocket = null;
//                int seq = 0;
            while (true) {
                try {
                    newsocket = serverSocket.accept();


                    DataInputStream dd1 = new DataInputStream(newsocket.getInputStream());
                    String servermsg = null;
                    do {
                        servermsg = dd1.readUTF();
                        Log.d("serverfirst", servermsg);
                    } while (servermsg == null);

                    String msgToadd[] = servermsg.split("//");
                    String s1 = msgToadd[0];
                    String idStr = msgToadd[1];

                    int idnum = Integer.valueOf(idStr);
                    Log.d("numberaya", Integer.toString(idnum));
                    float p = getMaxPriority(p1);
                    Log.d("initialpr", Float.toString(p));
                    p++;
                    if (MAXlocalPriority < p)
                        MAXlocalPriority = p;
                    Message m1 = new Message(p, s1, idnum);
                    p1.add(m1);
                    Log.d("addingtoPQ", p1.peek().msg + String.valueOf(p1.peek().id));
                    float send = p;
                    Log.d("prioritysendserver", Float.toString(send));

                    DataOutputStream dd = new DataOutputStream(newsocket.getOutputStream());
                    String strToSend = Float.toString(send);
                    dd.writeUTF(strToSend);
                    dd.flush();

                    Log.d("ACKissend", strToSend);

                    DataInputStream dd2 = new DataInputStream(newsocket.getInputStream());
                    String s2 = null;
                    try {
                        do {
                            s2 = dd2.readUTF();
                        } while (s2 == null);
                    } catch (IOException e) {
                        Log.d(TAG, "Exception Caught!");
                        e.printStackTrace();
                        continue;
                    }

                    String finalpr[] = s2.split("//");
                    float finalp = Float.valueOf(finalpr[0]);
                    Log.d("finalprserver/receives", Float.toString(finalp));
                    String finalMessage = finalpr[1];
                    Log.d("finalmsg", finalMessage);
                    if (MAXAgreedPriority < finalp)
                        MAXAgreedPriority = finalp;
                    Iterator<Message> pQueueIterator = p1.iterator();

                    while (pQueueIterator.hasNext()) {
                        Message clientMessage = pQueueIterator.next();
                        if (clientMessage.msg.equals(finalMessage)) {
                            Log.d("messageremovehorha", clientMessage.msg);
                            p1.remove(clientMessage);
                            break;
                        }

                    }
                    Message m2 = new Message(finalp, finalMessage, true);

                    p1.add(m2);

                    Iterator<Message> pQueueIterator1 = p1.iterator();
                    Log.v("newtest", String.valueOf(p1.size()));
                    Log.d("thefinalqueue", p1.peek().pr + " " + p1.peek().msg);
                    while (p1.peek() != null && p1.peek().isdecided) {
                        Message temp = p1.poll();
                        seq++;
                        ContentValues cv = new ContentValues();
                        cv.put(KEY_FIELD, Integer.toString(seq));
                        cv.put(VALUE_FIELD, temp.msg);
                        getContentResolver().insert(mUri, cv);
                        Log.v("addingfinal", seq + " " + temp.pr + " " + temp.msg);
                    }
                }catch (IOException e) {
                    e.printStackTrace();

                }


            }
        }

        protected void onProgressUpdate(String... strings) {

            String strReceived = strings[0].trim();
            TextView remoteTextView = (TextView) findViewById(R.id.textView1);
            remoteTextView.append(strReceived + "\t\n");

            return;
        }
    }

    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {
            LinkedList<Float> l1 = new LinkedList<Float>();
            LinkedList<Message>  exceptionLinkedList = new LinkedList<Message>();

//            try {
            String msgToSend=null;
            String[] remotePort = {REMOTE_PORT0, REMOTE_PORT1, REMOTE_PORT2, REMOTE_PORT3, REMOTE_PORT4};
            Socket socket[] = new Socket[5];
            for (int i = 0; i < remotePort.length; i++) {
                try {
                    socket[i] = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(remotePort[i]));
                    // socket[i].setSoTimeout(20000);
                    msgToSend = msgs[0];
                    Log.d("readingclient", msgToSend);

                    DataOutputStream dd = new DataOutputStream(socket[i].getOutputStream());
                    dd.writeUTF(msgToSend+"//"+Integer.toString(GlobalValue.ProcessId));
                    Log.d("Clientfirst", msgToSend);

                    DataInputStream br = new DataInputStream(socket[i].getInputStream());
                    float prsend = 0.0f;
                    String s3 = null;
                    Log.d("Client", "Hereeee");
                    Log.v("aisecheck", String.valueOf(i));
                    Log.d("Client", "Waiting for acknowledgement");
                    s3 = br.readUTF();
                    Log.d("prisREAD", s3);

                    prsend = (Float.valueOf(s3));
                    Log.d("ClientReadingPr", Float.toString(prsend));
                    Log.v("prsend", String.valueOf(prsend));

                    prsend = (int) prsend + i * 0.1f;
                    Log.v("prsend", String.valueOf(prsend));
                    Log.v("prsend", String.valueOf(i));
                    Log.v("prsend", String.valueOf(i * 0.1f));
                    l1.add(prsend);
                    Log.v("addingtolinkedlist", Float.toString(prsend));

                }catch(Exception out){
                    Log.d("yahan aya","avd dead hui"+" "+Integer.toString(i)+socket[i]);
                    Log.d("kuchnh", String.valueOf(p1.size()));
                    exceptionLinkedList.clear();
                    Iterator<Message> exceptionIterator = p1.iterator();
                    while(exceptionIterator.hasNext()) {
                        Message exceptionMessage = exceptionIterator.next();
                        Log.d("Iteratorbna","ptanh");
                        Log.d("idmila", String.valueOf(exceptionMessage.id));
                        if (exceptionMessage.id == i) {
                            Log.d("Loop Enter ho gya", Integer.toString(exceptionMessage.id) + Integer.toString(i));
                            exceptionLinkedList.add(exceptionMessage);
                            Log.d("was msg removed", "whyyyyy");
                            // it shouldn't break here right because we need to remove all messages
                        }
                    }
                }

            }
            for( Message i:exceptionLinkedList) {
                p1.remove(i);
            }




            Collections.sort(l1);
            float decided=l1.getLast();
            Log.d("decidedpr",Float.toString(decided));
            try {
                for (int i = 0; i < remotePort.length; i++) {
                    String finalTosend = decided + "//" + msgToSend;
                    Log.d("finalmsgserver", finalTosend);
                    DataOutputStream dd3 = new DataOutputStream(socket[i].getOutputStream());

                    dd3.writeUTF(finalTosend);

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
//            } catch (UnknownHostException e) {
//                Log.e(TAG, "ClientTask UnknownHostException");
//            } catch (IOException e) {
//                Log.e(TAG, "ClientTask socket IOException");
//            }
//            catch (Exception e){
//                   Log.e(TAG,"Client Exception");
//            }
            return null;
        }

    }
}
