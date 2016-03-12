package com.giovanny.bles2;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
/*Primera version de lectura*/
public class MainActivity extends AppCompatActivity {
    private BluetoothAdapter mBluetoothAdapter;
    private int REQUEST_ENABLE_BT = 1;
    private BluetoothLeScanner mLEScanner;
    private ScanSettings settings;
    private List<ScanFilter> filters;
    private boolean activado;
    private int TiempoCiclo=3000;

    private ArrayList<BluetoothDevice> arrayBluetoothD;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE Not Supported",Toast.LENGTH_SHORT).show();
            finish();
        }
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        arrayBluetoothD=new ArrayList<>();
    }

    @Override
    protected void onResume() {
        super.onResume();
        activado=true;
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            if (Build.VERSION.SDK_INT >= 21) {
                mLEScanner = mBluetoothAdapter.getBluetoothLeScanner();
                settings = new ScanSettings.Builder()
                        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                        .build();
                filters = new ArrayList<>();
            }
            mLEScanner.startScan(filters, settings, mScanCallback);
            hiloBeacon();
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            //scanLeDevice(false);
            mLEScanner.stopScan(mScanCallback);
            activado =false;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        activado=false;
        mLEScanner.stopScan(mScanCallback);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_CANCELED) {
                finish();
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private ScanCallback mScanCallback = new ScanCallback() {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            //Log.i("callbackType", String.valueOf(callbackType));
            //Log.i("result", result.toString());
            addDevice(result.getDevice());
        }
    };

    private synchronized void addDevice(BluetoothDevice btDevice){
        for(BluetoothDevice actual: arrayBluetoothD){
            if(actual.getAddress().equals(btDevice.getAddress())){
                return;
            }
        }
        Log.d("Scaner", "_Anadi:" + btDevice.getName());
        arrayBluetoothD.add(btDevice);
    }

    public synchronized Object getArrayBluetoothD() {
        return arrayBluetoothD.clone();
    }

    public synchronized void resetArrayBluetoothD() {
        this.arrayBluetoothD.clear();
    }

    public void Aparecio(boolean cond,BluetoothDevice bldv){
        if(cond){
            Log.d("Scaner","Aparecio :"+ bldv.getName());
            Toast.makeText(this, "Aparecio : "+bldv.getName(),
                    Toast.LENGTH_SHORT).show();
        }
        else {
            Log.d("Scaner","Desaparecio! : " + bldv.getName());
            Toast.makeText(this, "Desaparecio! : " + bldv.getName(),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void hiloBeacon() {

        new Thread() {

            ArrayList<BluetoothDevice> anteriores= (ArrayList<BluetoothDevice>) getArrayBluetoothD();

            public void run() {
                try {
                    Thread.sleep(TiempoCiclo/3);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                for(BluetoothDevice ant: anteriores){
                    Aparecio(true,ant);
                }

                while (activado) {
                    try {
                        Thread.sleep(TiempoCiclo);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    String ants="";
                    for(BluetoothDevice ant: anteriores){
                        ants+=ant.getName()+"_";
                    }
                    Log.d("Scaner","un ciclo ,_"+ants);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ArrayList<BluetoothDevice> nuevos= (ArrayList<BluetoothDevice>) getArrayBluetoothD();
                            boolean []ban=new boolean[anteriores.size()];
                            Arrays.fill(ban, false);

                            int j=0;

                            for(BluetoothDevice actual: nuevos){
                                int i;
                                for(i=0;i<anteriores.size();i++){
                                    if(actual.getAddress().equals(anteriores.get(i).getAddress())){
                                        ban[i]=true;
                                        break;
                                    }
                                }
                                if(i==anteriores.size())
                                    Aparecio(true,actual);
                                j++;
                            }

                            j=0;
                            for(BluetoothDevice ant: anteriores){
                                if(ban[j]==false)
                                    Aparecio(false,ant);
                                j++;
                            }
                            anteriores= (ArrayList<BluetoothDevice>) nuevos.clone();
                            resetArrayBluetoothD();
                        }
                    });
                }
            }


        }.start();
    }

}
