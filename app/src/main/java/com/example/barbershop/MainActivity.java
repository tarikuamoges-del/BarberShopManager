package com.example.barbershop;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    LinearLayout root, content; TextView total, count; double sales=0; int services=0;
    ArrayList<String> records=new ArrayList<>();

    public void onCreate(Bundle b){super.onCreate(b); showDashboard();}

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
        String[] x={"Dashboard","New Sale","Services","Employees","Reports"};
        for(String s:x){Button b=btn(s); n.addView(b,new LinearLayout.LayoutParams(0,60,1));
            if(s.equals("Dashboard")) b.setOnClickListener(v->showDashboard());
            if(s.equals("New Sale")) b.setOnClickListener(v->newSale());
            if(s.equals("Services")) b.setOnClickListener(v->services());
            if(s.equals("Employees")) b.setOnClickListener(v->employees());
            if(s.equals("Reports")) b.setOnClickListener(v->reports());
        }
        root.addView(n);
    }
    void showDashboard(){
        base("💈 BARBER SHOP MANAGER");
        content.addView(tv("Today",16));
        total=tv(String.format("Sales: ETB %.2f",sales),28); content.addView(total);
        count=tv("Services completed: "+services,20); content.addView(count);
        content.addView(tv("\nQuick actions",20));
        Button b=btn("＋ Record New Customer / Service"); content.addView(b); b.setOnClickListener(v->newSale());
        Button r=btn("View Today's Report"); content.addView(r); r.setOnClickListener(v->reports());
        content.addView(tv("\nRecent transactions",20));
        for(String s:records) content.addView(tv(s,15));
        nav();
    }
    void newSale(){
        base("New Service");
        EditText customer=new EditText(this); customer.setHint("Customer name / phone"); content.addView(customer);
        Spinner service=new Spinner(this);
        String[] sv={"Haircut","Beard","Hair + Beard","Hair Wash","Hair + Beard + Wash","Other"};
        service.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,sv)); content.addView(service);
        EditText price=new EditText(this); price.setHint("Price (ETB)"); price.setInputType(2); content.addView(price);
        EditText barber=new EditText(this); barber.setHint("Barber / employee"); content.addView(barber);
        Button save=btn("SAVE SERVICE"); content.addView(save);
        save.setOnClickListener(v->{
            double p=0; try{p=Double.parseDouble(price.getText().toString());}catch(Exception e){}
            sales+=p; services++;
            String now=new SimpleDateFormat("HH:mm").format(new Date());
            records.add(0,now+" • "+customer.getText()+" • "+service.getSelectedItem()+" • ETB "+p+" • "+barber.getText());
            Toast.makeText(this,"Service recorded",Toast.LENGTH_SHORT).show(); showDashboard();
        });
        nav();
    }
    void services(){
        base("Services & Prices");
        String[] sv={"Haircut — set your price","Beard — set your price","Hair + Beard — set your price","Hair Wash — set your price","Full Service — set your price"};
        for(String s:sv) content.addView(tv(s,19));
        content.addView(tv("\nTip: replace these with your actual Addis prices.",14));
        nav();
    }
    void employees(){
        base("Employees");
        content.addView(tv("Add and track barbers",20));
        EditText e=new EditText(this); e.setHint("Employee name"); content.addView(e);
        Button add=btn("Add Employee"); content.addView(add);
        add.setOnClickListener(v->{ if(e.length()>0){content.addView(tv("✓ "+e.getText(),18)); e.setText("");}});
        nav();
    }
    void reports(){
        base("Daily Report");
        content.addView(tv(new SimpleDateFormat("dd MMM yyyy").format(new Date()),18));
        content.addView(tv(String.format("TOTAL SALES\nETB %.2f",sales),28));
        content.addView(tv("SERVICES\n"+services,22));
        content.addView(tv("\nTransaction details",20));
        for(String s:records) content.addView(tv(s,14));
        nav();
    }
}
