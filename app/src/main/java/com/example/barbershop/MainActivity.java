package com.example.barbershop;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import android.content.Intent;
import android.net.Uri;
import java.io.OutputStream;
import java.util.*;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;
public class MainActivity extends Activity {
    GoogleSignInClient googleClient;
    LinearLayout root, content; TextView total, count; double sales=0; int services=0;
    ArrayList<String> records=new ArrayList<>();
    android.content.SharedPreferences prefs;

    public void onCreate(Bundle b){super.onCreate(b); prefs=getSharedPreferences("BarberShopData",MODE_PRIVATE); loadData(); setupGoogleSignIn(); scheduleAutomaticBackup(); showDashboard();}
    void setupGoogleSignIn(){ GoogleSignInOptions options=new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().requestScopes(new com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/drive.file")).build(); googleClient=GoogleSignIn.getClient(this,options); }

    void scheduleAutomaticBackup(){ PeriodicWorkRequest request=new PeriodicWorkRequest.Builder(BackupWorker.class,24,TimeUnit.HOURS).build(); WorkManager.getInstance(this).enqueueUniquePeriodicWork("BarberShopAutomaticBackup",androidx.work.ExistingPeriodicWorkPolicy.KEEP,request); }
    TextView tv(String s,int size){ TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setPadding(20,18,20,18); return t; }
    Button btn(String s){ Button b=new Button(this); b.setText(s); b.setAllCaps(false); return b; }

    void base(String title){
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        TextView bar=tv(title,22); bar.setTextColor(Color.WHITE); bar.setBackgroundColor(Color.rgb(25,25,25));
        root.addView(bar,new LinearLayout.LayoutParams(-1,70));
        content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(16,16,16,16);
        ScrollView sv=new ScrollView(this); sv.addView(content); root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
    }
    void nav(){
        LinearLayout n=new LinearLayout(this);
        String[] x={"Dashboard","New Sale","Services","Employees","Reports","Backup"};
        for(String s:x){Button b=btn(s); n.addView(b,new LinearLayout.LayoutParams(0,60,1));
            if(s.equals("Dashboard")) b.setOnClickListener(v->showDashboard());
            if(s.equals("New Sale")) b.setOnClickListener(v->newSale());
            if(s.equals("Services")) b.setOnClickListener(v->services());
            if(s.equals("Employees")) b.setOnClickListener(v->employees());
            if(s.equals("Reports")) b.setOnClickListener(v->reports());
            if(s.equals("Backup")) b.setOnClickListener(v->backup());
        }
        root.addView(n);
    }
    void backup(){ base("Backup & Restore"); content.addView(tv("Backup and restore your BarberShopManager data.",18)); Button b=btn("Create Backup"); content.addView(b); b.setOnClickListener(v->createBackup()); Button r=btn("Restore Backup"); content.addView(r); r.setOnClickListener(v->restoreBackup()); nav(); }
    void createBackup(){ Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT); intent.setType("application/json"); intent.putExtra(Intent.EXTRA_TITLE,"BarberShop_Backup_"+new SimpleDateFormat("yyyy-MM-dd_HH-mm").format(new Date())+".json"); startActivityForResult(intent,2001); }
    void restoreBackup(){ Intent intent=new Intent(Intent.ACTION_OPEN_DOCUMENT); intent.setType("application/json"); intent.addCategory(Intent.CATEGORY_OPENABLE); startActivityForResult(intent,2002); }
    void showDashboard(){
        base("💈 BARBER SHOP MANAGER");
        content.addView(tv("Today",16));
        total=tv(String.format("Sales: ETB %.2f",sales),28); content.addView(total);
          count=tv("Customers served: "+services,20); content.addView(count);
        content.addView(tv("\nQuick actions",20));
        Button b=btn("＋ Record New Customer / Service"); content.addView(b); b.setOnClickListener(v->newSale());
        Button r=btn("View Today's Report"); content.addView(r); r.setOnClickListener(v->reports());
        content.addView(tv("Recent transactions",20));
        recordsTable();
        nav();
    }
    void newSale(){
        base("New Service");

        EditText customer=new EditText(this);
        customer.setHint("Customer name / phone");
        content.addView(customer);

        Spinner service=new Spinner(this);
          String[] defaults={"Haircut","Beard","Hair + Beard","Hair Wash","Full Service"};
          String deletedServices=prefs.getString("deleted_services","");
          ArrayList<String> serviceList=new ArrayList<>();
          for(String s:defaults){ if(!deletedServices.contains("|"+s+"|")) serviceList.add(s); }
          String customServices=prefs.getString("custom_services","");
        if(!customServices.isEmpty()){
            for(String s:customServices.split("\\|")){
                if(!s.isEmpty() && !serviceList.contains(s)) serviceList.add(s);
            }
        }
        String[] sv=serviceList.toArray(new String[0]);
        service.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,sv));
        content.addView(service);

        EditText price=new EditText(this);
        price.setEnabled(false);
        price.setHint("Price (ETB)");
        price.setInputType(2);
        content.addView(price);

        String savedEmployees=prefs.getString("employees","");
        String[] employeeList;
        if(savedEmployees.isEmpty()){
            employeeList=new String[]{"No employees added"};
        }else{
            employeeList=savedEmployees.split("\\|");
        }

        Spinner barber=new Spinner(this);
        barber.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,employeeList));
        content.addView(barber);

        service.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){
            public void onItemSelected(android.widget.AdapterView<?> parent,android.view.View view,int position,long id){
                String selected=sv[position];
                String savedPrice=prefs.getString("price_"+selected,"");
                price.setText(savedPrice);
            }
            public void onNothingSelected(android.widget.AdapterView<?> parent){}
        });

        Spinner payment=new Spinner(this);
        String[] payments={"Cash","Card","Transfer","Other"};
        payment.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,payments));
        content.addView(payment);
        Button save=btn("SAVE SERVICE");
        content.addView(save);

        save.setOnClickListener(v->{
            double p=0;
            try{p=Double.parseDouble(price.getText().toString());}catch(Exception e){}

            if(p<=0){
                Toast.makeText(this,"Please set a price first",Toast.LENGTH_SHORT).show();
                return;
            }

              int customerNo=prefs.getInt("customer_no",0)+1;
              prefs.edit().putInt("customer_no",customerNo).apply();
            sales+=p;
                services++;
            String paymentMethod=payment.getSelectedItem().toString();

                String barberName=barber.getSelectedItem().toString();
            String now=new SimpleDateFormat("HH:mm").format(new Date());
              records.add(0,"#"+customerNo+" • "+now+" • "+customer.getText()+" • "+service.getSelectedItem()+" • ETB "+p+" • "+barberName+" • "+paymentMethod);

            saveData();

            Toast.makeText(this,"Service recorded",Toast.LENGTH_SHORT).show();
            showDashboard();
        });

        nav();
    }
      void services(){
          base("Services & Prices");
          String[] defaults={"Haircut","Beard","Hair + Beard","Hair Wash","Full Service"};
          String deleted=prefs.getString("deleted_services","");
          for(String name:defaults){
              if(!deleted.contains("|"+name+"|")) addServiceRow(name,false);
          }
          String custom=prefs.getString("custom_services","");
          if(!custom.isEmpty()){
              for(String name:custom.split("\\|")){
                  if(!name.isEmpty()) addServiceRow(name,true);
              }
          }
          Button add=btn("Add New Service");
          content.addView(add);
          add.setOnClickListener(v->{
              EditText name=new EditText(this);
              name.setHint("New service name");
              content.addView(name);
              EditText price=new EditText(this);
              price.setHint("Price (ETB)");
              price.setInputType(2);
              content.addView(price);
              Button save=btn("Save New Service");
              content.addView(save);
              save.setOnClickListener(x->{
                  String n=name.getText().toString().trim();
                  String pr=price.getText().toString().trim();
                  if(n.isEmpty() || pr.isEmpty()){
                      Toast.makeText(this,"Enter service name and price",Toast.LENGTH_SHORT).show();
                      return;
                  }
                  String old=prefs.getString("custom_services","");
                  String updated=old.isEmpty()?n:old+"|"+n;
                  prefs.edit().putString("custom_services",updated).putString("price_"+n,pr).apply();
                  services();
              });
          });
          nav();
      }

    void addServiceRow(String name,boolean custom){
        LinearLayout row=new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        TextView label=tv(name,18);
        row.addView(label,new LinearLayout.LayoutParams(0,70,1));
        EditText price=new EditText(this);
        price.setHint("ETB");
        price.setInputType(2);
        price.setText(prefs.getString("price_"+name,""));
        row.addView(price,new LinearLayout.LayoutParams(180,70));
        Button save=btn("Save");
        row.addView(save,new LinearLayout.LayoutParams(100,70));
        Button delete=btn("Delete");
        row.addView(delete,new LinearLayout.LayoutParams(110,70));
        save.setOnClickListener(v->{
            prefs.edit().putString("price_"+name,price.getText().toString()).apply();
            Toast.makeText(this,name+" price saved",Toast.LENGTH_SHORT).show();
        });
          delete.setOnClickListener(v->{
              new android.app.AlertDialog.Builder(this)
                  .setTitle("Delete Service")
                  .setMessage("Are you sure you want to delete "+name+"?")
                  .setNegativeButton("Cancel",null)
                  .setPositiveButton("Delete",(d,w)->{
                      if(custom){
                          String old=prefs.getString("custom_services","");
                          StringBuilder updated=new StringBuilder();
                          for(String x:old.split("\\|")){
                              if(!x.equals(name) && !x.isEmpty()){
                                  if(updated.length()>0) updated.append("|");
                                  updated.append(x);
                              }
                          }
                          prefs.edit().putString("custom_services",updated.toString()).remove("price_"+name).apply();
                      }else{
                          String deleted=prefs.getString("deleted_services","");
                          if(!deleted.contains("|"+name+"|")){
                              deleted=deleted+name+"|";
                              if(!deleted.startsWith("|")) deleted="|"+deleted;
                          }
                          prefs.edit().putString("deleted_services",deleted).remove("price_"+name).apply();
                      }
                      services();
                  }).show();
          });
            content.addView(row);
        }
      void employees(){
          EditText e=new EditText(this);
        e.setHint("Employee name");
        content.addView(e);
        Button add=btn("Add Employee");
        content.addView(add);
        add.setOnClickListener(v->{
            if(e.length()>0){
                String name=e.getText().toString().trim();
                String old=prefs.getString("employees","");
                String updated=old.isEmpty()?name:old+"|"+name;
                prefs.edit().putString("employees",updated).apply();
                e.setText("");
                employees();
            }
        });
        String saved=prefs.getString("employees","");
        if(!saved.isEmpty()){
            content.addView(tv("Current Employees",20));
            for(String name:saved.split("\\|")){
                if(!name.isEmpty()){
                    LinearLayout row=new LinearLayout(this);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.addView(tv("✓ "+name,18),new LinearLayout.LayoutParams(0,70,1));
                    Button del=btn("Delete");
                    row.addView(del,new LinearLayout.LayoutParams(120,70));
                    del.setOnClickListener(v->{
                        new android.app.AlertDialog.Builder(this)
                            .setTitle("Delete Employee")
                            .setMessage("Delete "+name+"?")
                            .setNegativeButton("Cancel",null)
                            .setPositiveButton("Delete",(d,w)->{
                                String current=prefs.getString("employees","");
                                StringBuilder updated=new StringBuilder();
                                for(String x:current.split("\\|")){
                                    if(!x.equals(name) && !x.isEmpty()){
                                        if(updated.length()>0) updated.append("|");
                                        updated.append(x);
                                    }
                                }
                                prefs.edit().putString("employees",updated.toString()).apply();
                                employees();
                            }).show();
                    });
                    content.addView(row);
                }
            }
        }
        nav();
    }
    void recordsTable(){
        HorizontalScrollView scroll=new HorizontalScrollView(this);
        TableLayout table=new TableLayout(this);
        table.setStretchAllColumns(false);
        TableRow header=new TableRow(this);
        String[] heads={"#","Time","Customer","Service","Price","Barber","Payment"};
        for(String h:heads){
            TextView t=tv(h,15); t.setSingleLine(true);
            t.setTextColor(Color.WHITE);
            t.setBackgroundColor(Color.rgb(50,50,50));
            header.addView(t,new TableRow.LayoutParams(180,70));
        }
        table.addView(header);
        for(String record:records){
            String[] p=record.split(" • ",-1);
            TableRow row=new TableRow(this);
            for(int i=0;i<7;i++){
                String value=i<p.length?p[i]:"";
                TextView t=tv(value,14); t.setSingleLine(true);
                row.addView(t,new TableRow.LayoutParams(180,70));
            }
            table.addView(row);
        }
        scroll.addView(table);
        content.addView(scroll,new LinearLayout.LayoutParams(-1,-2));
    }
    void reports(){
        base("Daily Report");
        content.addView(tv(new SimpleDateFormat("dd MMM yyyy").format(new Date()),18));
        content.addView(tv(String.format("TOTAL SALES\nETB %.2f",sales),28));
        content.addView(tv("SERVICES\n"+services,22));
        Button download=btn("Download Report (CSV)");
        content.addView(download);
        download.setOnClickListener(v->downloadReport());
        recordsTable();
        nav();
    }
    void downloadReport(){
        Intent intent=new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_TITLE,"BarberShop_Report_"+new SimpleDateFormat("yyyy-MM-dd_HH-mm").format(new Date())+".csv");
        startActivityForResult(intent,1001);
    }
    @Override protected void onActivityResult(int requestCode,int resultCode,android.content.Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==2002 && resultCode==RESULT_OK && data!=null){ try{ Uri uri=data.getData(); java.io.InputStream in=getContentResolver().openInputStream(uri); java.io.BufferedReader reader=new java.io.BufferedReader(new java.io.InputStreamReader(in,"UTF-8")); StringBuilder text=new StringBuilder(); String line; while((line=reader.readLine())!=null) text.append(line); reader.close(); org.json.JSONObject json=new org.json.JSONObject(text.toString()); android.content.SharedPreferences.Editor edit=prefs.edit(); java.util.Iterator<String> keys=json.keys(); while(keys.hasNext()){ String key=keys.next(); Object value=json.get(key); if(value instanceof Integer) edit.putInt(key,((Integer)value).intValue()); else if(value instanceof Long) edit.putLong(key,((Long)value).longValue()); else if(value instanceof Boolean) edit.putBoolean(key,((Boolean)value).booleanValue()); else if(value instanceof Float) edit.putFloat(key,((Float)value).floatValue()); else edit.putString(key,String.valueOf(value)); } edit.apply(); loadData(); Toast.makeText(this,"Backup restored successfully. Please reopen the screen.",Toast.LENGTH_LONG).show(); showDashboard(); }catch(Exception e){ Toast.makeText(this,"Could not restore backup",Toast.LENGTH_SHORT).show(); } }
        if(requestCode==2001 && resultCode==RESULT_OK && data!=null){ try{ Uri uri=data.getData(); OutputStream out=getContentResolver().openOutputStream(uri); String json=new org.json.JSONObject(prefs.getAll()).toString(2); out.write(json.getBytes("UTF-8")); out.close(); Toast.makeText(this,"Backup saved successfully",Toast.LENGTH_SHORT).show(); }catch(Exception e){ Toast.makeText(this,"Could not save backup",Toast.LENGTH_SHORT).show(); } }
        if(requestCode==1001 && resultCode==RESULT_OK && data!=null){
            try{
                Uri uri=data.getData();
                OutputStream out=getContentResolver().openOutputStream(uri);
                StringBuilder csv=new StringBuilder();
                csv.append("#,Time,Customer,Service,Price,Barber,Payment\n");
                for(String record:records){
                    String[] p=record.split(" • ",-1);
                    for(int i=0;i<7;i++){
                        String value=i<p.length?p[i]:"";
                        csv.append("\"").append(value.replace("\"","\"\"")).append("\"");
                        if(i<6) csv.append(",");
                    }
                    csv.append("\n");
                }
                out.write(csv.toString().getBytes("UTF-8"));
                out.close();
                Toast.makeText(this,"Report saved successfully",Toast.LENGTH_SHORT).show();
            }catch(Exception e){
                Toast.makeText(this,"Could not save report",Toast.LENGTH_SHORT).show();
            }
        }
    }
    void saveData(){
        getSharedPreferences("BarberShopData",MODE_PRIVATE).edit().putString("sales",String.valueOf(sales)).putInt("services",services).putString("records",join(records)).apply();
    }
    String join(ArrayList<String> list){
        StringBuilder r=new StringBuilder();
        for(String s:list){if(r.length()>0)r.append("|");r.append(s.replace("|"," "));}
        return r.toString();
    }
    ArrayList<String> split(String text){
        ArrayList<String> r=new ArrayList<>();
        if(text==null||text.isEmpty())return r;
        for(String s:text.split("\\|"))r.add(s);
        return r;
    }
    void loadData(){
        sales=Double.parseDouble(prefs.getString("sales","0"));
        services=prefs.getInt("services",0);
        records=split(prefs.getString("records",""));
    }
}
