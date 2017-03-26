package julioarellano.wiiselandroid7.utils;

import android.app.Service;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import julioarellano.wiiselandroid7.R;

public class CustomToast {
    private static Toast toast;
    
    public static Toast makeText(Context c, int resource, int duration){
        return makeText(c, c.getString(resource), duration);
    }
    
    public static Toast makeText(Context c, String message, int duration){
        LayoutInflater inflater = (LayoutInflater) c.getSystemService(Service.LAYOUT_INFLATER_SERVICE);
        View toastView = inflater.inflate(R.layout.customtoast_layout, null);
        
        TextView tvMessage = (TextView) toastView.findViewById(R.id.message);
        tvMessage.setText(message);
        
        toast = new Toast(c);
        toast.setDuration(duration);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.setView(toastView);
        
        return toast;
    }
    
}
