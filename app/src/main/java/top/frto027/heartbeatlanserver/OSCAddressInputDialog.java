package top.frto027.heartbeatlanserver;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.net.InetAddress;
import java.net.UnknownHostException;

import kotlin.text.Regex;

public class OSCAddressInputDialog extends DialogFragment {
    interface Listener{
        void onOk(String ip, int port);
        void onCancel();
    }
    private EditText inputIp, inputPort;

    private Listener listener;

    public OSCAddressInputDialog setListener(Listener listener){
        this.listener = listener;
        return this;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = requireActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.osc_add_layout, null);
        inputIp = view.findViewById(R.id.osc_ip_input_et);
        inputPort = view.findViewById(R.id.osc_port_input_et);
        inputPort.setText(getActivity().getPreferences(Context.MODE_PRIVATE).getString("oscLastPort", "9000"));
        builder.setView(view)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        //check ip format
                        try{
                            String ip = inputIp.getText().toString();
                            if(ip.equals(""))
                                ip = inputIp.getHint().toString();
                            InetAddress.getByName(ip);
                            int port = Integer.parseInt(inputPort.getText().toString());
                            SharedPreferences.Editor editor = getActivity().getPreferences(Context.MODE_PRIVATE).edit();
                            editor.putString("oscLastPort",port+"");
                            editor.apply();
                            if(listener != null){
                                listener.onOk(ip, port);
                            }
                        }catch (UnknownHostException e){
                            Toast.makeText(getActivity(), "Unknown host", Toast.LENGTH_LONG);
                            if(listener != null) listener.onCancel();
                        }catch (NumberFormatException e){
                            Toast.makeText(getActivity(), "Invalid port", Toast.LENGTH_LONG);
                            if(listener != null) listener.onCancel();
                        }
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        OSCAddressInputDialog.this.getDialog().cancel();
                        if(listener != null) listener.onCancel();

                    }
                });
        return builder.create();
    }
}
