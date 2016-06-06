package com.distributie.adapters;

import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.distributie.beans.Etapa;
import com.distributie.beans.EvenimentNou;
import com.distributie.dialog.CustomAlertDialog;
import com.distributie.dialog.CustomInfoDialog;
import com.distributie.enums.EnumNetworkStatus;
import com.distributie.enums.EnumOpConfirm;
import com.distributie.enums.EnumOperatiiEvenimente;
import com.distributie.enums.EnumTipEtapa;
import com.distributie.enums.TipEveniment;
import com.distributie.listeners.AlertDialogListener;
import com.distributie.listeners.BorderouriListener;
import com.distributie.listeners.OperatiiBorderouriListener;
import com.distributie.listeners.OperatiiEtapeListener;
import com.distributie.listeners.OperatiiEvenimenteListener;
import com.distributie.model.OperatiiBorderouriDAOImpl;
import com.distributie.model.OperatiiEvenimente;
import com.distributie.model.UserInfo;
import com.distributie.utils.Constants;
import com.distributie.utils.DateUtils;
import com.distributie.utils.MapUtils;
import com.distributie.view.R;
import com.google.android.gms.maps.model.LatLng;

public class EtapeAdapter extends BaseAdapter implements OperatiiEvenimenteListener, OperatiiBorderouriListener, AlertDialogListener {

	private List<Etapa> listEtape;
	private Context context;
	private OperatiiEvenimente opEvenimente;
	private ViewHolder currentView;
	private int currentPosition;
	private Etapa etapaCurenta;
	private OperatiiBorderouriDAOImpl opBorderouri;
	private CustomAlertDialog alertDialog;
	private OperatiiEtapeListener etapeListener;
	private BorderouriListener borderouriListener;

	public EtapeAdapter(Context context, List<Etapa> listEtape) {
		this.context = context;
		this.listEtape = listEtape;

		opEvenimente = new OperatiiEvenimente(context);
		opEvenimente.setOperatiiEvenimenteListener(EtapeAdapter.this);

		opBorderouri = new OperatiiBorderouriDAOImpl(context);
		opBorderouri.setEventListener(this);

		alertDialog = new CustomAlertDialog(context);
		alertDialog.setAlertDialogListener(this);

		checkEtapeRamase();

	}

	static class ViewHolder {
		public TextView textNrCrt, textNumeEtapa, textDescEtapa, textDocument;
		public Button btnSalvareEveniment, btnAnulareEveniment;
		public ImageView checkedIcon;

	}

	public void setListEtape(List<Etapa> listEtape) {
		this.listEtape = listEtape;
		notifyDataSetChanged();
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		final ViewHolder viewHolder;

		if (convertView == null) {
			convertView = LayoutInflater.from(context).inflate(R.layout.item_etapa, parent, false);

			viewHolder = new ViewHolder();
			viewHolder.textNrCrt = (TextView) convertView.findViewById(R.id.textNrCrt);
			viewHolder.textNumeEtapa = (TextView) convertView.findViewById(R.id.textNumeEtapa);
			viewHolder.textDescEtapa = (TextView) convertView.findViewById(R.id.textDescEtapa);
			viewHolder.btnSalvareEveniment = (Button) convertView.findViewById(R.id.btnSalvareEveniment);
			viewHolder.btnAnulareEveniment = (Button) convertView.findViewById(R.id.btnAnulareEveniment);
			viewHolder.textDocument = (TextView) convertView.findViewById(R.id.textDocument);
			viewHolder.checkedIcon = (ImageView) convertView.findViewById(R.id.checkedIcon);

			convertView.setTag(viewHolder);

		} else {
			viewHolder = (ViewHolder) convertView.getTag();
		}

		final Etapa etapa = getItem(position);

		if (!etapa.getNume().contains("SFARSIT") && !etapa.getNume().contains("INCEPUT")) {
			viewHolder.btnSalvareEveniment.setText("SOSIRE");
		} else {
			viewHolder.btnSalvareEveniment.setText(etapa.getNume());

		}

		currentView = viewHolder;
		currentPosition = position;

		setSaveButtonsStatus(currentPosition, currentView);

		viewHolder.textNrCrt.setText(String.valueOf(position + 1) + ".");
		viewHolder.textNumeEtapa.setText(etapa.getNume());
		viewHolder.textDescEtapa.setText(etapa.getDescriere());

		viewHolder.textDocument.setText("Nr. borderou " + etapa.getDocument());

		viewHolder.btnSalvareEveniment.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				saveEvent(position, viewHolder);
			}
		});

		viewHolder.btnAnulareEveniment.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				cancelEvent(position, viewHolder);
			}
		});

		if (position % 2 == 0)
			convertView.setBackgroundResource(R.drawable.shadow_dark);
		else
			convertView.setBackgroundResource(R.drawable.shadow_light);
		return convertView;
	}

	private void saveEvent(int position, ViewHolder viewHolder) {

		currentView = viewHolder;
		currentPosition = position;
		etapaCurenta = listEtape.get(position);

		if (etapaCurenta.getTipEtapa() == EnumTipEtapa.SOSIRE) {
			if (hasStartCursa())
				getPozitieCurenta();
			else
				showStartCursaDialog();
		} else {
			dealSaveEvent();
		}

	}

	private void getPozitieCurenta() {

		HashMap<String, String> params = new HashMap<String, String>();
		params.put("nrBorderou", listEtape.get(currentPosition).getDocument());

		opBorderouri.getPozitieCurenta(params);
	}

	private void showStartCursaDialog() {
		String infoText = "Marcati mai intai evenimentul Inceput cursa.";
		showInfoDialog(infoText);
	}

	private void dealSaveEvent() {
		switch (etapaCurenta.getTipEtapa()) {
		case SFARSIT_INCARCARE:
			setSfarsitIncarcareEv(etapaCurenta.getDocument());
			break;
		case START_BORD:
			saveStartStopEvent(etapaCurenta.getTipEtapa());
			break;
		case STOP_BORD:
			showSfarsitCursaAlertDialog();
			break;
		case SOSIRE:
			performSaveNewEventClienti();
			break;
		default:
			break;

		}

	}

	private void checkEtapeRamase() {
		int etapeRamase = 0;

		for (Etapa etapa : listEtape) {

			if (etapa.getTipEtapa() == EnumTipEtapa.SFARSIT_INCARCARE && !etapa.isSalvata()) {
				etapeRamase = 100;

			}

			if (etapa.getTipEtapa() == EnumTipEtapa.START_BORD && !etapa.isSalvata()) {
				etapeRamase = 100;

			}

			if (etapa.getTipEtapa() == EnumTipEtapa.SOSIRE && !etapa.isSalvata())
				etapeRamase++;

		}

		if (etapeRamase <= 2 && listEtape.size() > 0) {
			if (borderouriListener != null)
				borderouriListener.verificaBordeoruri();
		}

	}

	private void dealCancelEvent() {

		HashMap<String, String> params = new HashMap<String, String>();

		params.put("tipEveniment", etapaCurenta.getTipEtapa().toString());
		params.put("nrDocument", etapaCurenta.getDocument());
		params.put("codClient", etapaCurenta.getCodClient());
		params.put("codSofer", UserInfo.getInstance().getId());

		opBorderouri.cancelEvent(params);

	}

	private void performSaveNewEventClienti() {

		EvenimentNou ev = new EvenimentNou();
		ev.setCodSofer(UserInfo.getInstance().getId());
		ev.setDocument(listEtape.get(currentPosition).getDocument());
		ev.setClient(listEtape.get(currentPosition).getCodClient());
		ev.setCodAdresa(listEtape.get(currentPosition).getCodAdresaClient());
		ev.setEveniment("0");
		ev.setTipEveniment(TipEveniment.NOU);
		ev.setData(DateUtils.getCurrentDate());
		ev.setOra(DateUtils.getCurrentTime());

		opBorderouri.saveNewEventClient(ev);

	}

	private void showAnuleazaAlertDialog() {

		alertDialog.setTipOperatie(EnumOpConfirm.SOSIRE);
		alertDialog.setAlertText("Confirmati anularea ?");
		alertDialog.show();

	}

	private void showSfarsitCursaAlertDialog() {

		if (hasStartCursa()) {
			alertDialog.setTipOperatie(EnumOpConfirm.SFARSIT_CURSA);
			alertDialog.setAlertText("Aceasta operatie nu poate fi anulata. Borderoul curent nu va mai putea fi modificat. Confirmati sfarsit cursa ?");
			alertDialog.show();
		}

	}

	private boolean hasStartCursa() {

		for (Etapa etapa : listEtape) {
			if (etapa.getTipEtapa() == EnumTipEtapa.START_BORD && !etapa.isSalvata())
				return false;
		}

		return true;
	}

	private void saveStartStopEvent(EnumTipEtapa tipEtapa) {

		String tipEveniment = "";
		if (tipEtapa == EnumTipEtapa.START_BORD)
			tipEveniment = "0";
		else if (tipEtapa == EnumTipEtapa.STOP_BORD)
			tipEveniment = "P";

		OperatiiBorderouriDAOImpl newEvent = new OperatiiBorderouriDAOImpl(context);
		newEvent.setEventListener(EtapeAdapter.this);

		HashMap<String, String> newEventData = new HashMap<String, String>();
		newEventData.put("codSofer", UserInfo.getInstance().getId());
		newEventData.put("document", listEtape.get(currentPosition).getDocument());
		newEventData.put("client", listEtape.get(currentPosition).getDocument());
		newEventData.put("codAdresa", " ");
		newEventData.put("eveniment", tipEveniment);

		newEvent.saveNewEventBorderou(newEventData);

	}

	private void setSfarsitIncarcareEv(String codBorderou) {

		HashMap<String, String> params = new HashMap<String, String>();
		params.put("document", codBorderou);
		params.put("codSofer", UserInfo.getInstance().getId());
		opEvenimente.setSfarsitIncarcare(params);

	}

	private void cancelEvent(final int position, final ViewHolder viewHolder) {
		currentView = viewHolder;
		currentPosition = position;
		etapaCurenta = listEtape.get(position);
		showAnuleazaAlertDialog();
	}

	private void setSaveButtonsStatus(int position, ViewHolder currentView) {
		if (listEtape.get(position).isSalvata()) {
			currentView.btnSalvareEveniment.setVisibility(View.GONE);
			currentView.checkedIcon.setVisibility(View.VISIBLE);
			currentView.btnAnulareEveniment.setVisibility(View.VISIBLE);
		} else {
			currentView.btnSalvareEveniment.setVisibility(View.VISIBLE);
			currentView.btnAnulareEveniment.setVisibility(View.INVISIBLE);
			currentView.checkedIcon.setVisibility(View.GONE);
		}

	}

	private void handleSfarsitIncarcareEvent(String result) {

		// sf. incarcare produs
		if (result.contains("SOF")) {
			listEtape.get(currentPosition).setSalvata(true);
			setSaveButtonsStatus(currentPosition, currentView);

		} else {

		}

	}

	private void computeClientDistance(String coords) {

		boolean rangeOk = false;
		double distClient = 0;

		String[] strCoords = coords.split("x");
		LatLng latlngCurent = new LatLng(Double.valueOf(strCoords[0]), Double.valueOf(strCoords[1]));

		if (latlngCurent.latitude != 0) {
			LatLng latlngClient = null;

			try {
				latlngClient = MapUtils.geocodeSimpleAddress(listEtape.get(currentPosition).getFactura().getAdresaClient(), context);
			} catch (Exception e) {
				rangeOk = true;
			}

			if (latlngClient.latitude != 0) {
				distClient = MapUtils.distanceXtoY(latlngCurent.latitude, latlngCurent.longitude, latlngClient.latitude, latlngClient.longitude, "K");

				if (distClient > Constants.RAZA_SOSIRE_CLIENT_KM) {
					rangeOk = false;
				} else
					rangeOk = true;
			} else
				rangeOk = true;
		} else {
			rangeOk = true;
		}

		if (rangeOk)
			dealSaveEvent();
		else {
			String infoText = "Evenimentul nu poate fi salvat. Aceasta adresa se afla la " + Math.round(distClient) + " km fata de pozitia curenta.";
			showInfoDialog(infoText);
		}

	}

	private void showInfoDialog(String infoText) {
		CustomInfoDialog infoDialog = new CustomInfoDialog(context);
		infoDialog.setInfoText(infoText);
		infoDialog.show();

	}

	private void handleSavedEvent() {

		if (listEtape.get(currentPosition).getTipEtapa() == EnumTipEtapa.STOP_BORD) {
			if (etapeListener != null)
				etapeListener.borderouTerminat();
		} else {
			listEtape.get(currentPosition).setSalvata(true);
			setSaveButtonsStatus(currentPosition, currentView);

			checkEtapeRamase();

		}
	}

	private void handleCancelEvent() {
		listEtape.get(currentPosition).setSalvata(false);
		setSaveButtonsStatus(currentPosition, currentView);
	}

	public void setOperatiiEtapeListener(OperatiiEtapeListener etapeListener) {
		this.etapeListener = etapeListener;
	}

	public void setBorderouriListener(BorderouriListener listener) {
		this.borderouriListener = listener;
	}

	@Override
	public int getCount() {
		return listEtape.size();
	}

	@Override
	public Etapa getItem(int index) {
		return listEtape.get(index);
	}

	@Override
	public long getItemId(int arg0) {
		return 0;
	}

	@Override
	public void opEventComplete(String result, EnumOperatiiEvenimente methodName) {
		switch (methodName) {
		case SET_SFARSIT_INC:
			handleSfarsitIncarcareEvent(result);
			break;
		default:
			break;
		}

	}

	@Override
	public void eventComplete(String result, EnumOperatiiEvenimente methodName, EnumNetworkStatus networkStatus) {
		switch (methodName) {
		case SAVE_NEW_EVENT:
			handleSavedEvent();
			break;
		case CANCEL_EVENT:
			handleCancelEvent();
			break;
		case GET_POZITIE:
			computeClientDistance(result);
			break;
		default:
			break;

		}

	}

	@Override
	public void alertDialogOk(EnumOpConfirm tipOperatie) {
		switch (tipOperatie) {
		case SOSIRE:
			dealCancelEvent();
			break;
		case SFARSIT_CURSA:
			saveStartStopEvent(etapaCurenta.getTipEtapa());
			break;
		default:
			break;
		}

	}

}