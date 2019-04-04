package ba.unsa.etf.rma.klase;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import ba.unsa.etf.rma.R;

public class AdapterZaListuKvizova extends BaseAdapter {
    Context context;
    ArrayList<Kviz> data;
    private static LayoutInflater inflater = null;

    public AdapterZaListuKvizova(Context context, ArrayList<Kviz> data) {
        // TODO Auto-generated constructor stub
        this.context = context;
        this.data = data;
        inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
    public View getView(int position, View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub
        View vi = convertView;
        if (vi == null)
            vi = inflater.inflate(R.layout.element_liste_kvizova, null);
        TextView text = (TextView) vi.findViewById(R.id.Itemname);
        ImageView imageView = (ImageView) vi.findViewById( R.id.icon );
        text.setText( data.get(position).getNaziv() );
        return vi;
    }
}
