package ba.unsa.etf.rma.klase;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.maltaisn.icondialog.IconHelper;

import java.util.ArrayList;

import ba.unsa.etf.rma.R;
import ba.unsa.etf.rma.aktivnosti.DodajPitanjeAkt;

public class AdapterZaListuOdgovora extends BaseAdapter implements View.OnClickListener {

    private Context context;
    private ArrayList<String> data;
    private static LayoutInflater inflater = null;
    private ImageView ikonaClanaListe;

    public AdapterZaListuOdgovora(Context context, ArrayList<String> data) {
        // TODO Auto-generated constructor stub
        this.context = context;
        this.data = data;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        // TODO Auto-generated method stub
        return data.size();
    }

    @Override
    public Object getItem(int position) {
        // TODO Auto-generated method stub
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        // TODO Auto-generated method stub
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub
        View vi = convertView;
        if (vi == null)
            vi = inflater.inflate(R.layout.element_liste_kvizova, null);
        final TextView text = vi.findViewById(R.id.Itemname);
        ikonaClanaListe = vi.findViewById( R.id.icon );
        text.setText( data.get(position) );
        final IconHelper iconHelper = IconHelper.getInstance(vi.getContext());
        final View finalVi = vi;
        iconHelper.addLoadCallback(new IconHelper.LoadCallback() {
            @Override
            public void onDataLoaded() {
                text.setTextColor(Color.BLACK);
                ikonaClanaListe.setImageDrawable( finalVi.getResources().getDrawable( R.drawable.blue_dot ) );
                if(DodajPitanjeAkt.tacanOdgovor != null) {
                    if (data.get(position).equals(DodajPitanjeAkt.tacanOdgovor))
                        text.setTextColor(Color.GREEN);
                    else
                        text.setTextColor(Color.BLACK);
                }
                notifyDataSetChanged();
            }
        });
        return vi;
    }

    @Override
    public void onClick(View v) {
        //Do nothing
    }
}
