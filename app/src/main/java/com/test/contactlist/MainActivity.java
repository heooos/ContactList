package com.test.contactlist;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnItemClickListener {

    private ListView listView;
    private SimpleAdapter simpleAdapter;
    private List<Map<String, Object>> dataList;
    private EditText etName, etNumber;

    private String mNumber;

    private static final int REQUEST_READ_CONTACTS = 111;
    private static final int REQUEST_CALL_PHONE = 222;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        listView = (ListView) findViewById(R.id.listView);
        dataList = new ArrayList<>();
        simpleAdapter = new SimpleAdapter(MainActivity.this, dataList, R.layout.layout, new String[]{"head", "tvName", "tvNumber"}, new int[]{R.id.head, R.id.tvName, R.id.tvNumber});
        listView.setAdapter(simpleAdapter);
        listView.setOnItemClickListener(MainActivity.this);
        findViewById(R.id.btn_Add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddDialog();
            }
        });


    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    protected void onStart() {
        if (PermissionUtils.requestPermission(this, Manifest.permission.READ_CONTACTS)) {
            if (shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)) {
                AlertDialog dialog;
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("权限申请");
                builder.setMessage("读取通讯录权限为该应用正常使用的必须权限");
                builder.setPositiveButton("请求权限", new DialogInterface.OnClickListener() {
                    @TargetApi(Build.VERSION_CODES.M)
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, REQUEST_READ_CONTACTS);
                        dialog.dismiss();
                    }
                });
                builder.setNegativeButton("仍然拒绝", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(MainActivity.this, "无权限,程序退出", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                });
                builder.setCancelable(false);
                dialog = builder.create();
                dialog.show();
            }else {
                requestPermissions(new String[]{Manifest.permission.READ_CONTACTS}, REQUEST_READ_CONTACTS);
            }
        } else {
            readContacts();
        }
        super.onStart();
    }

    //添加联系人的自定义对话框
    private void showAddDialog() {
        LayoutInflater inflater = LayoutInflater.from(this);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final View view = inflater.inflate(R.layout.add_dialog_layout, null);
        builder.setTitle(R.string.add_label);
        builder.setView(view);
        //注意：此处的findViewById方法必须写在dialog的view加载之后！且注意调用的父对象不再是Activity中的view而是当前的View
        etName = (EditText) view.findViewById(R.id.et_Name);
        etNumber = (EditText) view.findViewById(R.id.et_Number);
        builder.setPositiveButton(R.string.btn_submit, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = etName.getText().toString();
                String number = etNumber.getText().toString();
                addContacts(name, number);  //调用添加联系人的方法
                listView.setOnItemClickListener(MainActivity.this);

            }
        });
        builder.setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(MainActivity.this, R.string.btn_cancel, Toast.LENGTH_SHORT).show();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    //向通讯录中数据库加联系人的方法
    public void addContacts(String name, String number) {
        ContentResolver resolver = getContentResolver();
        Uri uri = Uri.parse("content://com.android.contacts/raw_contacts");
        Uri dataUri = Uri.parse("content://com.android.contacts/data");

        ContentValues values = new ContentValues();
        Cursor cursor = resolver.query(uri, new String[]{"_id"}, null, null, null);
        cursor.moveToLast();
        int lastId = cursor.getInt(0);
        int newId = lastId + 1;
        values.put("contact_id", newId);
        resolver.insert(uri, values);

        ContentValues phoneValues = new ContentValues();
        phoneValues.put("data1", number);
        phoneValues.put("mimetype", "vnd.android.cursor.item/phone_v2");
        phoneValues.put("raw_contact_id", newId);
        resolver.insert(dataUri, phoneValues);

        ContentValues nameValues = new ContentValues();
        nameValues.put("data1", name);
        nameValues.put("mimetype", "vnd.android.cursor.item/name");
        nameValues.put("raw_contact_id", newId);
        readContacts();
        simpleAdapter.notifyDataSetChanged();

    }

    //ListView点击事件方法
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        ListView listview = (ListView) parent;

        HashMap<String, Object> data = (HashMap<String, Object>) listview.getItemAtPosition(position);
        //String name = (String) data.get("tvName");

        String number = (String) data.get("tvNumber");
        showClickDialog(number);
    }


    //点击listview后的自定义对话框
    private void showClickDialog(final String number) {
        LayoutInflater inflater = LayoutInflater.from(this);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final View view = inflater.inflate(R.layout.click_layout, null);
        builder.setTitle(R.string.choose_label);
        builder.setView(view);
        //注意：此处的findViewById方法必须写在dialog的view加载之后！且注意调用的父对象不再是Activity中的view而是当前的View
        view.findViewById(R.id.call).setOnClickListener(new View.OnClickListener() {
            @TargetApi(Build.VERSION_CODES.M)
            @Override
            public void onClick(View v) {
                mNumber = number;
                if (PermissionUtils.requestPermission(MainActivity.this, Manifest.permission.CALL_PHONE)) {
                    requestPermissions(new String[]{Manifest.permission.CALL_PHONE}, REQUEST_CALL_PHONE);
                } else {
                    callAndSMS(Intent.ACTION_CALL, Uri.parse("tel:" + mNumber));
                }
            }
        });
        view.findViewById(R.id.message).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                callAndSMS(Intent.ACTION_SENDTO, Uri.parse("smsto:" + number));
            }
        });
        builder.setNegativeButton(R.string.btn_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(MainActivity.this, R.string.btn_cancel, Toast.LENGTH_SHORT).show();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * 启动电话或者短信界面
     * @param actionCall
     * @param parse
     */
    private void callAndSMS(String actionCall, Uri parse) {
        Intent intent = new Intent();
        //系统默认的action，用来打开默认的电话界面
        intent.setAction(actionCall);
        intent.setData(parse);
        MainActivity.this.startActivity(intent);
    }


    //  读取系统的联系人列表。
    public void readContacts() {
        //   List<Map<String, Object>> list =null;
        Cursor cursor = MainActivity.this.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null, null, null, null);
        String phoneNumber;
        String phoneName;
        dataList.clear();
        while (cursor != null && cursor.moveToNext()) {

            phoneNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));//电话号码
            phoneName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));//姓名

            Map<String, Object> map = new HashMap<>();

            map.put("head", R.drawable.contact);
            map.put("tvName", phoneName);
            map.put("tvNumber", phoneNumber);
            dataList.add(map);
        }
        simpleAdapter.notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        menu.add("111");
        menu.add("222");

        return true;
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {

        switch (requestCode) {
            case REQUEST_READ_CONTACTS:
                if (permissions[0].equals(Manifest.permission.READ_CONTACTS) && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    readContacts();
                } else {
                    customDialog();
                }
                break;
            case REQUEST_CALL_PHONE:
                if (permissions[0].equals(Manifest.permission.CALL_PHONE) && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    callAndSMS(Intent.ACTION_DIAL, Uri.parse("tel:" + mNumber));
                } else {
                    Toast.makeText(this, "无权限,程序退出", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
        }

    }

    /**
     * 自定义权限申请窗口
     */
    private void customDialog(){
        AlertDialog dialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("权限申请");
        builder.setMessage("读取通讯录权限为该应用正常使用的必须权限");
        builder.setPositiveButton("跳转到设置", new DialogInterface.OnClickListener() {
            @TargetApi(Build.VERSION_CODES.M)
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_SETTINGS);
                startActivity(intent);
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("仍然拒绝", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(MainActivity.this, "无权限,程序退出", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
        builder.setCancelable(false);
        dialog = builder.create();
        dialog.show();
    }

}

