package com.distributie.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.widget.Toast;

import com.distributie.beans.BeanSofer;
import com.distributie.enums.EnumNetworkStatus;
import com.distributie.enums.EnumOperatiiSofer;
import com.distributie.listeners.AsyncTaskListener;
import com.distributie.listeners.SoferiListener;

public class OperatiiSoferi implements AsyncTaskListener {

	private Context context;
	private EnumOperatiiSofer numeOperatie;
	private SoferiListener soferiListener;

	public OperatiiSoferi(Context context) {
		this.context = context;
	}

	public void getSoferi() {
		numeOperatie = EnumOperatiiSofer.GET_SOFERI;
		HashMap<String, String> params = new HashMap<String, String>();
		performOperation(numeOperatie, params);

	}

	public void getMasinaSofer(HashMap<String, String> params) {
		numeOperatie = EnumOperatiiSofer.GET_MASINA;
		performOperation(numeOperatie, params);

	}

	public void getMasiniFiliala(HashMap<String, String> params) {
		numeOperatie = EnumOperatiiSofer.GET_MASINI_FILIALA;
		performOperation(numeOperatie, params);

	}

	public void getKmMasina(HashMap<String, String> params) {
		numeOperatie = EnumOperatiiSofer.GET_KM_MASINA;
		performOperation(numeOperatie, params);

	}

	public void adaugaKmMasina(HashMap<String, String> params) {
		numeOperatie = EnumOperatiiSofer.ADAUGA_KM_MASINA;
		performOperation(numeOperatie, params);

	}

	public void getKmMasinaDeclarati(HashMap<String, String> params) {
		numeOperatie = EnumOperatiiSofer.GET_KM_MASINA_DECLARATI;
		performOperation(numeOperatie, params);

	}

	public void valideazaKmMasina(HashMap<String, String> params) {
		numeOperatie = EnumOperatiiSofer.VALIDEAZA_KM_MASINA;
		performOperation(numeOperatie, params);

	}

	public void verificaKmMasina(HashMap<String, String> params) {
		numeOperatie = EnumOperatiiSofer.GET_STARE_VALIDARE_KM;
		performOperation(numeOperatie, params);

	}

	private void performOperation(EnumOperatiiSofer numeOperatie, HashMap<String, String> params) {
		AsyncTaskWSCall call = new AsyncTaskWSCall(numeOperatie.getNume(), params, (AsyncTaskListener) this, context);
		call.getCallResults();
	}

	public void setSoferiListener(SoferiListener listener) {
		this.soferiListener = listener;
	}

	@Override
	public void onTaskComplete(String methodName, String result, EnumNetworkStatus networkStatus) {

		if (soferiListener != null) {
			soferiListener.operationSoferiComplete(numeOperatie, result);
		}

	}

	public List<BeanSofer> decodJsonSoferi(String strSoferi) {
		List<BeanSofer> listSoferi = new ArrayList<BeanSofer>();

		try {
			JSONArray jsonObject = new JSONArray(strSoferi);

			for (int i = 0; i < jsonObject.length(); i++) {
				JSONObject soferObject = jsonObject.getJSONObject(i);

				BeanSofer unSofer = new BeanSofer();

				unSofer.setNume(soferObject.getString("nume"));
				unSofer.setFiliala(soferObject.getString("filiala"));
				unSofer.setCodTableta(soferObject.getString("codTableta"));

				listSoferi.add(unSofer);

			}

		} catch (JSONException e) {
			Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show();
		}

		return listSoferi;
	}

}
