package com.example.t_tazhan.production;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.t_tazhan.production.Image.ImageBrowseActivity;
import com.example.t_tazhan.production.util.AzureMLClient;
import com.example.t_tazhan.production.util.FileSave;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

import static com.example.t_tazhan.production.Image.ImageBrowseActivity.initData;
import static com.example.t_tazhan.production.Image.ImgBrowsePagerAdapter.height;
//import static com.example.t_tazhan.production.Image.ImgBrowsePagerAdapter.layoutContent;
import static com.example.t_tazhan.production.Image.ImgBrowsePagerAdapter.width;
import static com.example.t_tazhan.production.util.AzureMLClient.getPoint;
import static com.example.t_tazhan.production.util.AzureMLClient.requestResponse;
import static com.example.t_tazhan.production.util.AzureMLClient.transferBeacon;
import static com.example.t_tazhan.production.util.Constant.getBeacon;
import static com.example.t_tazhan.production.util.Constant.ifConclude;
import static com.example.t_tazhan.production.util.Constant.map;
import static com.example.t_tazhan.production.util.Constant.storageValue;

public class MainActivity extends AppCompatActivity {

    public Handler handler;
    BluetoothAdapter bluetoothAdapter1;
    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    Button buttonStart;
    Button buttonEnd;
    EditText textX;
    EditText textY;
    EditText textTimer;
    TextView textView;
    int countTime = 0;
    List<String> lst_Devices = new ArrayList<>();
    StringBuilder sb1 = new StringBuilder();
    String X = null,Y = null;
    String timerDuration = "8";
    private static TextView showView;//把数据display
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public  void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice bluetooth_Device ;
            //System.out.println("未成功进入receive状态"+"\r");
            //bug because not working
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                bluetooth_Device = intent
                        .getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (bluetooth_Device.getBondState() == BluetoothDevice.BOND_NONE) {
                    String str =
                            getBeacon(bluetooth_Device.getAddress()) + " "
                                    + intent.getExtras().getShort(bluetooth_Device.EXTRA_RSSI)
                                    + " ";
                   // System.out.println("A中设备信息&RSSI： "+str);
                   // System.out.println("\r");
                    if (lst_Devices.indexOf(str) == -1 &&
                            getBeacon(bluetooth_Device.getAddress()) != "mac") {// 防止地址被重复添加
                        lst_Devices.add(str);
                    }
                } else if(bluetooth_Device.getBondState() == BluetoothDevice.BOND_BONDED) {
                    String str =
                            getBeacon(bluetooth_Device.getAddress()) + " "
                                    + intent.getExtras().getShort(bluetooth_Device.EXTRA_RSSI)
                                    + " ";
                    //   System.out.println("B中设备信息&RSSI： "+str);
                    //   System.out.println("\r");
                    if (lst_Devices.indexOf(str) == -1 &&
                            getBeacon(bluetooth_Device.getAddress()) != "mac") {// 防止地址被重复添加
                        lst_Devices.add(str);
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        handler = new Handler();
        final BluetoothManager manager = (BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE);
        if (manager != null) {
            bluetoothAdapter1 = manager.getAdapter();
        }
        if (!bluetoothAdapter.isEnabled()) {
            int REQUEST_ENABLE_BT = 1;
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            Toast toast = Toast.makeText(MainActivity.this, "已经打开了蓝牙，可以正常使用APP", Toast.LENGTH_LONG);
            toast.show();
        }
        verifyStoragePermissions(this );
        textX = findViewById(R.id.x);
        textX.addTextChangedListener(textWatcher1);
        textY = findViewById(R.id.y);
        textY.addTextChangedListener(textWatcher2);
        textTimer = findViewById(R.id.inputTimer);
        textTimer.addTextChangedListener(textWatcher3);
        textView = findViewById(R.id.countTimer);
        textView.setText(String.valueOf(0));
        IntentFilter intent = new IntentFilter();
        intent.addAction(BluetoothDevice.ACTION_FOUND);
        intent.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intent.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        intent.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, intent);
        showView=findViewById(R.id.show_result);
    }
    public void onClick_Search(View v) {
        storageValue();
        startActivity(new Intent(this, ImageBrowseActivity.class));
        countTime = 0;
        startTimer();
        startTimer1();
    }
    public void onClick_End(View vie) {
        endFlag = true;
        sb1.append("此时信标位置为" + "[" + X + " "+ Y + "]").append("\r");
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        textX.getText().clear();
        textY.getText().clear();
        textTimer.getText().clear();
        textView.setText(String.valueOf(0));
    }
    public void onClick_Dump(View view){
        FileSave.saveFile(resultValue);
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            Toast toast = Toast.makeText(MainActivity.this, "已经打开了蓝牙，可以正常使用APP", Toast.LENGTH_LONG);
            toast.show();
        } else {
            Toast toast1 = Toast.makeText(MainActivity.this, "还没打开蓝牙，不能使用APP", Toast.LENGTH_LONG);
            toast1.show();
        }
    }
    protected void onDestroy() {
        super.onDestroy();
        if (bluetoothAdapter.isEnabled()) {
            bluetoothAdapter.disable();
        }
        unregisterReceiver(mReceiver);
    }
    boolean endFlag = false;

    private TextWatcher textWatcher1 = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            X = textX.getText().toString();
        }
    };
    private TextWatcher textWatcher2 = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            Y = textY.getText().toString();
        }
    };
    private TextWatcher textWatcher3 = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            timerDuration = textTimer.getText().toString();
        }
    };

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE };
    public static void verifyStoragePermissions(Activity activity) {
        int permission = ActivityCompat.checkSelfPermission(activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE);
        }
    }
    Timer timer;
    TimerTask timerTask;
    public void startTimer(){
        timer = new Timer();
        initializeTimerTask();
        timer.schedule(timerTask,0,10000);
    }
    public void initializeTimerTask() {
        timerTask = new TimerTask() {
            @Override
            public void run() {
                if (endFlag) {
                    return;
                }
                if (bluetoothAdapter.isDiscovering()) {
                    bluetoothAdapter.cancelDiscovery();
                }
                bluetoothAdapter.startDiscovery();
                for (int j = 0; j < lst_Devices.size(); j++) {
                    map.put(lst_Devices.get(j).split(" ")[0],lst_Devices.get(j).split(" ")[1]);
                }
                //debug for working？
                ifConclude(map);
                lst_Devices.clear();
            }
        };
    }
    Timer timer1;
    TimerTask timerTask1;
    public void startTimer1(){
        timer1 = new Timer();
        initializeTimerTask1();
        timer1.schedule(timerTask1,0,5000);
    }
    public void initializeTimerTask1() {
        timerTask1 = new TimerTask() {
            @Override
            public void run() {
                getBeaconMessage(map);//getbeacon failed
            }
        };
    }
    public  void getBeaconMessage(TreeMap<String,String> map){
        getAPIRequest(map);
        initData();
        updatePoint();
    }
    private void updatePoint() {
        runOnUiThread(new Runnable() {
            public void run() {
                layoutContent.addPoints(width,height);
            }
        });
    }atic String resultValue=new String();
    public static int locationX = 11;//定位的坐标点x
    public static int locationY = 6;//定位的坐标点y
    public static void getAPIRequest(TreeMap<String,String> map) {
        try {
            String temp1 = transferBeacon(map);
            String temp2 = requestResponse(temp1);
            String resultString = getPoint(temp2)+"\r";//单次取出Web返回的数据resultString
            resultValue+=resultString;//拼接整段数据
            System.out.println("保存的数据"+resultString+"\r");
            showView.setText(resultString);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
