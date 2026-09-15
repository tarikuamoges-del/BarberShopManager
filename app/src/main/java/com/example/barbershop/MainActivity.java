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
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
public class MainActivity extends Activity {
    GoogleSignInClient googleClient;
    LinearLayout root, content; TextView total, count; double sales=0; int services=0;
    ArrayList<String> records=new ArrayList<>(); ArrayList<String> salesLedger=new ArrayList<>();
ArrayList<String> cartItems=new ArrayList<>();
    android.content.SharedPreferences prefs;

    public void onCreate(Bundle b){super.onCreate(b); prefs=getSharedPreferences("BarberShopData",MODE_PRIVATE); loadData(); setupGoogleSignIn(); scheduleAutomaticBackup(); showDashboard();}
    void setupGoogleSignIn(){ GoogleSignInOptions options=new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().requestScopes(new com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/drive.file")).build(); googleClient=GoogleSignIn.getClient(this,options); }
    Drive getDriveService(GoogleSignInAccount account){ GoogleAccountCredential credential=GoogleAccountCredential.usingOAuth2(this,Collections.singleton("https://www.googleapis.com/auth/drive.file")); credential.setSelectedAccount(account.getAccount()); return new Drive.Builder(new NetHttpTransport(),new GsonFactory(),credential).setApplicationName("BarberShopManager").build(); }

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
        String[] x={"Dashboard","New Sale","Services","Employees","Inventory","Reports","Backup"};
        for(String s:x){Button b=btn(s); n.addView(b,new LinearLayout.LayoutParams(0,60,1));
            if(s.equals("Dashboard")) b.setOnClickListener(v->showDashboard());
            if(s.equals("New Sale")) b.setOnClickListener(v->newSale());
            if(s.equals("Services")) b.setOnClickListener(v->services());
            if(s.equals("Employees")) b.setOnClickListener(v->employees());
            if(s.equals("Inventory")) b.setOnClickListener(v->inventory());
            if(s.equals("Reports")) b.setOnClickListener(v->reports());
            if(s.equals("Backup")) b.setOnClickListener(v->backup());
        }
        root.addView(n);
    }
    void backup(){ base("Backup & Restore"); content.addView(tv("Backup and restore your BarberShopManager data.",18)); Button google=btn("Connect Google Drive"); content.addView(google); google.setOnClickListener(v->startActivityForResult(googleClient.getSignInIntent(),3001)); Button b=btn("Create Backup"); content.addView(b); b.setOnClickListener(v->createBackup()); Button r=btn("Restore Backup"); content.addView(r); r.setOnClickListener(v->restoreBackup()); nav(); }
    void createBackup(){ GoogleSignInAccount account=GoogleSignIn.getLastSignedInAccount(this); if(account==null){ Toast.makeText(this,"Please connect Google Drive first",Toast.LENGTH_SHORT).show(); return; } new Thread(()->{ try{ String json=new org.json.JSONObject(prefs.getAll()).toString(2); com.google.api.services.drive.Drive drive=getDriveService(account); java.util.List<com.google.api.services.drive.model.File> folders=drive.files().list().setQ("name='BarberShopManager' and mimeType='application/vnd.google-apps.folder' and trashed=false").setSpaces("drive").setFields("files(id,name)").execute().getFiles(); String folderId; if(folders!=null && !folders.isEmpty()){ folderId=folders.get(0).getId(); }else{ com.google.api.services.drive.model.File folder=new com.google.api.services.drive.model.File(); folder.setName("BarberShopManager"); folder.setMimeType("application/vnd.google-apps.folder"); folderId=drive.files().create(folder).setFields("id").execute().getId(); } com.google.api.services.drive.model.File fileMetadata=new com.google.api.services.drive.model.File(); fileMetadata.setName("BarberShop_Backup_"+new SimpleDateFormat("yyyy-MM-dd_HH-mm").format(new Date())+".json"); fileMetadata.setMimeType("application/json"); fileMetadata.setParents(java.util.Collections.singletonList(folderId)); com.google.api.client.http.ByteArrayContent content=new com.google.api.client.http.ByteArrayContent("application/json",json.getBytes("UTF-8")); com.google.api.services.drive.model.File uploaded=drive.files().create(fileMetadata,content).setFields("id,name,parents").execute(); drive.files().update(uploaded.getId(),new com.google.api.services.drive.model.File()).setAddParents(folderId).setRemoveParents("root").setFields("id,name,parents").execute(); runOnUiThread(()->Toast.makeText(this,"Backup uploaded. Parent: "+(uploaded.getParents()==null?"NONE":uploaded.getParents().toString()),Toast.LENGTH_LONG).show()); }catch(Exception e){ runOnUiThread(()->Toast.makeText(this,"Google Drive backup failed: "+e.getMessage(),Toast.LENGTH_LONG).show()); } }).start(); }
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
    base("New Sale");

    EditText customer=new EditText(this);
    customer.setHint("Customer name / phone");
    content.addView(customer);

    content.addView(tv("Add services:",20));

    String[] defaults={"Haircut","Beard","Hair + Beard","Hair Wash","Full Service"};
    String deletedServices=prefs.getString("deleted_services","");
    ArrayList<String> serviceList=new ArrayList<>();

    for(String s:defaults){
        if(!deletedServices.contains("|"+s+"|")) serviceList.add(s);
    }

    String customServices=prefs.getString("custom_services","");
    if(!customServices.isEmpty()){
        for(String s:customServices.split("\\|")){
            if(!s.isEmpty() && !serviceList.contains(s)) serviceList.add(s);
        }
    }
    for(String item:serviceList){
        Button add=btn(item+"  +"); content.addView(add); add.setOnClickListener(v->{ boolean found=false; for(int i=0;i<cartItems.size();i++){ String[] cp=cartItems.get(i).split("~",-1); if(cp.length>=2 && cp[0].equals(item)){ int newQty=Integer.parseInt(cp[1])+1; cartItems.set(i,item+"~"+newQty); found=true; break; } } if(!found) cartItems.add(item+"~1"); updateCartDisplay(); });
    }
    content.addView(tv("Add products:",20));
    String savedInventory=prefs.getString("inventory",""); if(!savedInventory.isEmpty()){ for(String inv:savedInventory.split("\\|")){ String[] p=inv.split("~",-1); if(p.length>=6){ String productName=p[0]; Button addProduct=btn(productName+"  +"); content.addView(addProduct); addProduct.setOnClickListener(v->{ boolean found=false; for(int i=0;i<cartItems.size();i++){ String[] cp=cartItems.get(i).split("~",-1); if(cp.length>=2 && cp[0].equals(productName)){ int newQty=Integer.parseInt(cp[1])+1; cartItems.set(i,productName+"~"+newQty); found=true; break; } } if(!found) cartItems.add(productName+"~1"); updateCartDisplay(); }); } } }
    String savedEmployees=prefs.getString("employees","");
    String[] employeeList;

    if(savedEmployees.isEmpty()){
        employeeList=new String[]{"No employees added"};
    }else{
        employeeList=savedEmployees.split("\\|");
    }

    Spinner barber=new Spinner(this);
    barber.setAdapter(new ArrayAdapter<String>(
        this,
        android.R.layout.simple_spinner_dropdown_item,
        employeeList
    ));
    content.addView(barber);

    Spinner payment=new Spinner(this);
    String[] payments={"Cash","Card","Transfer","Other"};
    payment.setAdapter(new ArrayAdapter<String>(
        this,
        android.R.layout.simple_spinner_dropdown_item,
        payments
    ));
    content.addView(payment);

    Button save=btn("SAVE SALE");
    content.addView(save);

    save.setOnClickListener(v->{
        if(cartItems.isEmpty()){
            Toast.makeText(this,"Please add at least one service",Toast.LENGTH_SHORT).show();
            return;
        }

        double cartTotal=0;
        StringBuilder saleServices=new StringBuilder();

        StringBuilder salePrices=new StringBuilder();
        for(String item:cartItems){
            String[] p=item.split("~",-1);
            if(p.length<2) continue;

            String name=p[0];
            int qty=Integer.parseInt(p[1]);
            double price=0;

            try{
                price=Double.parseDouble(prefs.getString("price_"+name,"0"));
            }catch(Exception e){}

            cartTotal+=price*qty;

            if(saleServices.length()>0) saleServices.append(", ");
            if(salePrices.length()>0) salePrices.append(", "); salePrices.append(name).append(" x").append(qty).append(" @").append(price);
            saleServices.append(name).append(" x").append(qty);
        }

        if(cartTotal<=0){
            Toast.makeText(this,"Please set prices for the selected services",Toast.LENGTH_SHORT).show();
            return;
        }

        int customerNo=prefs.getInt("customer_no",0)+1;
        prefs.edit().putInt("customer_no",customerNo).apply();

        sales+=cartTotal;
        services++;

        String paymentMethod=payment.getSelectedItem().toString();
        String barberName=barber.getSelectedItem().toString();
        String now=new SimpleDateFormat("HH:mm").format(new Date());

        records.add(0,
            "#"+customerNo+" • "+now+" • "+customer.getText()+" • "+
            saleServices+" • ETB "+cartTotal+" • "+barberName+" • "+paymentMethod
        );

        salesLedger.add(0,
            customerNo+"~"+now+"~"+customer.getText()+"~"+
            saleServices+"~"+cartTotal+"~"+barberName+"~"+paymentMethod+"~"+salePrices
        );

          for(String item:cartItems){ String[] cp=item.split("~",-1); if(cp.length>=2){ String product=cp[0]; int qty=Integer.parseInt(cp[1]); String inv=prefs.getString("inventory",""); StringBuilder updatedInv=new StringBuilder(); for(String stock:inv.split("\\|")){ if(stock.isEmpty()) continue; String[] sp=stock.split("~",-1); if(sp.length>=6 && sp[0].equals(product)){ double current=Double.parseDouble(sp[4]); sp[4]=String.valueOf(Math.max(0,current-qty)); stock=sp[0]+"~"+sp[1]+"~"+sp[2]+"~"+sp[3]+"~"+sp[4]+"~"+sp[5]; } if(updatedInv.length()>0) updatedInv.append("|"); updatedInv.append(stock); } prefs.edit().putString("inventory",updatedInv.toString()).apply(); } }
        cartItems.clear();
        saveData();

        Toast.makeText(this,"Sale recorded",Toast.LENGTH_SHORT).show();
        showDashboard();
    });

    nav();

    content.addView(tv("Cart",20));
    updateCartDisplay();

    }

void updateCartDisplay(){
    for(int i=content.getChildCount()-1;i>=0;i--){
        android.view.View v=content.getChildAt(i);
        if("CART_ITEM".equals(v.getTag()) || "CART_TOTAL".equals(v.getTag())){
            content.removeViewAt(i);
        }
    }
    double cartTotal=0;
    for(String item:cartItems){
        String[] p=item.split("~",-1);
        if(p.length<2) continue;
        String name=p[0];
        int qty=Integer.parseInt(p[1]);
        double price=0;
          try{ price=Double.parseDouble(prefs.getString("price_"+name,"0")); if(price==0){ String inv=prefs.getString("inventory",""); for(String stock:inv.split("\\|")){ String[] sp=stock.split("~",-1); if(sp.length>=6 && sp[0].equals(name)){ price=Double.parseDouble(sp[3]); break; } } } }catch(Exception e){}
        cartTotal+=price*qty;
        final String selectedName=name;
        LinearLayout row=new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setTag("CART_ITEM");
        TextView label=tv(name+"  x"+qty+"  • ETB "+(price*qty),16);
        Button minus=btn("−");
        Button plus=btn("+");
        row.addView(label,new LinearLayout.LayoutParams(0,70,1));
        row.addView(minus,new LinearLayout.LayoutParams(70,70));
        row.addView(plus,new LinearLayout.LayoutParams(70,70));
        content.addView(row);
        minus.setOnClickListener(v->{
            for(int i=0;i<cartItems.size();i++){
                String[] cp=cartItems.get(i).split("~",-1);
                if(cp.length>=2 && cp[0].equals(selectedName)){
                    int newQty=Integer.parseInt(cp[1])-1;
                    if(newQty<=0) cartItems.remove(i);
                    else cartItems.set(i,selectedName+"~"+newQty);
                    break;
                }
            }
            updateCartDisplay();
        });        plus.setOnClickListener(v->{
            for(int i=0;i<cartItems.size();i++){
                String[] cp=cartItems.get(i).split("~",-1);
                if(cp.length>=2 && cp[0].equals(selectedName)){
                    int newQty=Integer.parseInt(cp[1])+1;
                    cartItems.set(i,selectedName+"~"+newQty);
                    break;
                }
            }
            updateCartDisplay();
        });    }

    TextView totalView=tv("TOTAL: ETB "+String.format("%.2f",cartTotal),24);
    totalView.setTag("CART_TOTAL");
    content.addView(totalView);
}

    void inventory(){
        base("Inventory");
        content.addView(tv("Stock & Products",20));
        Button add=btn("Add Product");
        content.addView(add);
        add.setOnClickListener(v->{
            EditText name=new EditText(this);
            name.setHint("Product name");
            content.addView(name);
            EditText category=new EditText(this);
            category.setHint("Category");
            content.addView(category);
            EditText cost=new EditText(this);
            cost.setHint("Purchase cost (ETB)");
            cost.setInputType(2|8192);
            content.addView(cost);
            EditText sell=new EditText(this);
            sell.setHint("Selling price (ETB)");
            sell.setInputType(2|8192);
            content.addView(sell);
            EditText qty=new EditText(this);
            qty.setHint("Current quantity");
            qty.setInputType(2);
            content.addView(qty);
            EditText min=new EditText(this);
            min.setHint("Minimum stock level");
            min.setInputType(2);
            content.addView(min);
            Button save=btn("Save Product");
            content.addView(save);
            save.setOnClickListener(x->{
                String n=name.getText().toString().trim();
                String c=category.getText().toString().trim();
                String co=cost.getText().toString().trim();
                String se=sell.getText().toString().trim();
                String q=qty.getText().toString().trim();
                String m=min.getText().toString().trim();
                if(n.isEmpty()||co.isEmpty()||se.isEmpty()||q.isEmpty()||m.isEmpty()){
                    Toast.makeText(this,"Enter product name, prices and quantities",Toast.LENGTH_SHORT).show();
                    return;
                }
                String old=prefs.getString("inventory","");
                String item=n+"~"+c+"~"+co+"~"+se+"~"+q+"~"+m;
                String updated=old.isEmpty()?item:old+"|"+item;
                prefs.edit().putString("inventory",updated).apply();
                inventory();
            });
        });
        String saved=prefs.getString("inventory","");
        if(!saved.isEmpty()){
            content.addView(tv("Current Stock",20));
            for(String item:saved.split("\\|")){
                if(!item.isEmpty()){
                    String[] p=item.split("~",-1);
                    if(p.length>=6){
                        double quantity=0;
                        double minimum=0;
                        try{quantity=Double.parseDouble(p[4]);}catch(Exception e){}
                        try{minimum=Double.parseDouble(p[5]);}catch(Exception e){}
                        String status=quantity<=minimum?"  ⚠ LOW STOCK":"";
                    Button addStock=btn("+ Stock");
                    Button removeStock=btn("- Stock");
                    content.addView(addStock);
                    content.addView(removeStock);
                    addStock.setOnClickListener(v->{updateInventoryQuantity(p[0],Double.parseDouble(p[4])+1);});
                    removeStock.setOnClickListener(v->{updateInventoryQuantity(p[0],Math.max(0,Double.parseDouble(p[4])-1));});
                        content.addView(tv(p[0]+"  •  Qty: "+p[4]+"  •  Sell: ETB "+p[3]+status,17));
                    }
                }
            }
        }
        nav();
    }
    void updateInventoryQuantity(String product,double newQty){
        String saved=prefs.getString("inventory","");
        StringBuilder updated=new StringBuilder();
        for(String item:saved.split("\\|")){
            if(item.isEmpty()) continue;
            String[] p=item.split("~",-1);
            if(p.length>=6 && p[0].equals(product)){
                p[4]=String.valueOf(newQty);
                item=p[0]+"~"+p[1]+"~"+p[2]+"~"+p[3]+"~"+p[4]+"~"+p[5];
            }
            if(updated.length()>0) updated.append("|");
            updated.append(item);
        }
        prefs.edit().putString("inventory",updated.toString()).apply();
        Toast.makeText(this,"Stock updated",Toast.LENGTH_SHORT).show();
        inventory();
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
                CheckBox charge=new CheckBox(this); charge.setText("Count for employee service charge"); content.addView(charge);
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
                  prefs.edit().putString("custom_services",updated).putString("price_"+n,pr).putBoolean("charge_"+n,charge.isChecked()).apply();
                  services();
              });
          });
          nav();
      }

    void addServiceRow(String name,boolean custom){
          TextView label=tv(name,18);
        LinearLayout row=new LinearLayout(this);
          CheckBox charge=new CheckBox(this); charge.setText("Charge"); charge.setChecked(prefs.getBoolean("charge_"+name,false)); row.addView(charge,new LinearLayout.LayoutParams(140,70));
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
            prefs.edit().putString("price_"+name,price.getText().toString()).putBoolean("charge_"+name,charge.isChecked()).apply();
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
    void employees(){ base("Employees"); EditText e=new EditText(this);
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
          Button chargeBtn=btn("Calculate Service Charge"); content.addView(chargeBtn); chargeBtn.setOnClickListener(v->calculateServiceCharges());
        content.addView(tv("Employee Performance",20)); for(String emp:saved.split("\\|")){ if(!emp.isEmpty()){ int empCount=0; double empSales=0; for(String sale:salesLedger){ String[] p=sale.split("~",-1); if(p.length>=7 && p[5].equals(emp)){ empCount++; try{empSales+=Double.parseDouble(p[4]);}catch(Exception ex){} } } content.addView(tv(emp+"  •  Services: "+empCount+"  •  Sales: ETB "+String.format("%.2f",empSales),17)); } }
        nav();
    }
    void calculateServiceCharges(){
    for(int i=content.getChildCount()-1;i>=0;i--){ android.view.View v=content.getChildAt(i); if("SERVICE_CHARGE".equals(v.getTag())) content.removeViewAt(i); }
        TextView chargeTitle=tv("Employee Service Charges",20); chargeTitle.setTag("SERVICE_CHARGE"); content.addView(chargeTitle);
        String savedEmployees=prefs.getString("employees","");
        if(savedEmployees.isEmpty()){
            content.addView(tv("Add employees first.",17));
            return;
        }
        ArrayList<String> eligible=new ArrayList<>();
        String[] defaults={"Haircut","Beard","Hair + Beard","Hair Wash","Full Service"};
        String deleted=prefs.getString("deleted_services","");
        for(String s:defaults){
            if(!deleted.contains("|"+s+"|") && prefs.getBoolean("charge_"+s,false)) eligible.add(s);
        }
        String custom=prefs.getString("custom_services","");
        if(!custom.isEmpty()){
            for(String s:custom.split("\\|")){
                if(!s.isEmpty() && prefs.getBoolean("charge_"+s,false)) eligible.add(s);
            }
        }
        if(eligible.isEmpty()){
            TextView noCharge=tv("No services are marked for employee service charge.",17); noCharge.setTag("SERVICE_CHARGE"); content.addView(noCharge);
            return;
        }
        for(String emp:savedEmployees.split("\\|")){
            if(emp.isEmpty()) continue;
            TextView empTitle=tv("\n"+emp,19); empTitle.setTag("SERVICE_CHARGE"); content.addView(empTitle);
            for(String service:eligible){
                LinearLayout row=new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.addView(tv(service,16),new LinearLayout.LayoutParams(0,70,1));
                EditText pct=new EditText(this);
                pct.setHint("%");
                pct.setInputType(2|8192);
                pct.setText(prefs.getString("charge_pct_"+emp+"_"+service,""));
                row.addView(pct,new LinearLayout.LayoutParams(150,70));
                Button savePct=btn("Save");
                row.addView(savePct,new LinearLayout.LayoutParams(100,70));
                savePct.setOnClickListener(v->{
                    prefs.edit().putString("charge_pct_"+emp+"_"+service,pct.getText().toString()).apply();
                    Toast.makeText(this,"Percentage saved",Toast.LENGTH_SHORT).show();
                });
                row.setTag("SERVICE_CHARGE"); content.addView(row);
            }
            double employeeCharge=0;
            for(String sale:salesLedger){
                String[] p=sale.split("~",-1);
                if(p.length<7 || !p[5].equals(emp)) continue;
                String serviceText=p[3];
                for(String service:eligible){
                    String marker=service+" x";
                    for(String part:serviceText.split(", ")){
                        if(part.startsWith(marker)){
                            try{
                                int pos=part.lastIndexOf(" x");
                                int qty=Integer.parseInt(part.substring(pos+2).trim());
                                double price=0; if(p.length>=8){ for(String hp:p[7].split(", ")){ if(hp.startsWith(service+" x")){ try{ price=Double.parseDouble(hp.substring(hp.lastIndexOf("@")+1).trim()); }catch(Exception ex){} break; } } } if(price==0) price=Double.parseDouble(prefs.getString("price_"+service,"0"));
                                double pct=Double.parseDouble(prefs.getString("charge_pct_"+emp+"_"+service,"0"));
                                employeeCharge += price*qty*pct/100.0;
                            }catch(Exception ex){}
                        }
                    }
                }
            }
            TextView chargeTotal=tv("Calculated Service Charge: ETB "+String.format("%.2f",employeeCharge),18); chargeTotal.setTag("SERVICE_CHARGE"); content.addView(chargeTotal);
        }
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
                TextView t=tv(value,14); t.setSingleLine(false); t.setMaxLines(5); t.setEllipsize(null);
                row.addView(t,new TableRow.LayoutParams(180,-2));
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
        if(requestCode==3001){ if(resultCode==RESULT_OK && data!=null){ try{ GoogleSignInAccount account=GoogleSignIn.getSignedInAccountFromIntent(data).getResult(com.google.android.gms.common.api.ApiException.class); Toast.makeText(this,"Google Drive connected: "+account.getEmail(),Toast.LENGTH_LONG).show(); }catch(Exception e){ Toast.makeText(this,"Google Drive sign-in failed",Toast.LENGTH_SHORT).show(); } } else { Toast.makeText(this,"Google Drive sign-in cancelled",Toast.LENGTH_SHORT).show(); } }
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
        getSharedPreferences("BarberShopData",MODE_PRIVATE).edit().putString("sales",String.valueOf(sales)).putInt("services",services).putString("records",join(records)).putString("salesLedger",join(salesLedger)).apply();
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
        records=split(prefs.getString("records","")); salesLedger=split(prefs.getString("salesLedger",""));
    }
}
