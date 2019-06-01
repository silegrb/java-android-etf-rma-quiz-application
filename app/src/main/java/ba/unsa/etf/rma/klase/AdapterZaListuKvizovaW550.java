package ba.unsa.etf.rma.klase;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.maltaisn.icondialog.IconHelper;

import java.util.ArrayList;

import ba.unsa.etf.rma.R;

public class AdapterZaListuKvizovaW550 extends BaseAdapter implements View.OnClickListener {

    private Context context;
    private ArrayList<Kviz> data;
    private static LayoutInflater inflater = null;
    private ImageView ikonaClanaListe;

    public AdapterZaListuKvizovaW550(Context context, ArrayList<Kviz> data) {
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
            vi = inflater.inflate(R.layout.element_liste_kvizova_w550, null);
        TextView textOne = vi.findViewById(R.id.itemnameOne);
        TextView textTwo = vi.findViewById(R.id.itemnameTwo);
        ikonaClanaListe = vi.findViewById( R.id.icon );
        Kviz k = (Kviz)data.get(position);
        textOne.setText( k.getNaziv() );
        String brojPitanja;
        if(  k.getNaziv().equals("Dodaj kviz") )
            brojPitanja = "";
        else {
            brojPitanja = String.valueOf( k.getPitanja().size());
        }
        textTwo.setText( brojPitanja );
        final IconHelper iconHelper = IconHelper.getInstance(vi.getContext());
        final View finalVi = vi;
        iconHelper.addLoadCallback(new IconHelper.LoadCallback() {
            @Override
            public void onDataLoaded() {
                // This happens on UI thread, and is guaranteed to be called.
                boolean omgMoze = true;
                try{
                    data.get(position).getKategorija().getId();
                }
                catch (Exception e){
                    omgMoze = false;
                }
                if( !data.get(position).getNaziv().equals("Dodaj kviz") && omgMoze ) {
                    ikonaClanaListe.setImageDrawable(iconHelper.getIcon(Integer.parseInt(data.get(position).getKategorija().getId())).getDrawable(context));
                }
                else
                    ikonaClanaListe.setImageDrawable( finalVi.getResources().getDrawable( R.drawable.addicon ) );
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
