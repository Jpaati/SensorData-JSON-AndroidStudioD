package sensor;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import com.example.sensordata_json.R;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SensorActivity extends AppCompatActivity {
    private OkHttpClient client = null;
    final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    String appKey = "";
    String baseURl = "";
    String thing = "";
    String datatable = "";

        private JSONArray listAllSensors() throws JSONException {
            SensorManager sm = (SensorManager) getSystemService(SENSOR_SERVICE);
            List<Sensor> sensors = sm.getSensorList(Sensor.TYPE_ALL);
            JSONArray rows = new JSONArray();
            for (Sensor s : sensors)
            {
                String name = s.getName();
                String type = s.getStringType();
                String vendor = s.getVendor();
                String sensInfo =  String.format("%s: %s, %s", type, name, vendor);
                Log.d("SENSOR",sensInfo);

                rows.put(new JSONObject()
                        .put("type", type)
                        .put("name", name)
                        .put("vendor", vendor)
                        .put("info", sensInfo));
            }
            return rows;
        }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        client = new OkHttpClient();
        appKey = getString(R.string.app_key);
        baseURl = getString(R.string.url);
        thing = getString(R.string.thing);
        datatable = getString(R.string.data_table_sensor);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sensor);
    }

    public void writeOnServer(View view)
    {
        PutServerAsyncTask task  = new PutServerAsyncTask();
        task.execute();
    }
    public void getFromServer(View view)
    {
        GetServerAsyncTask task  = new GetServerAsyncTask();
        task.execute();
    }

    public void callService(View view)
    {
        CallServiceAsyncTask task = new CallServiceAsyncTask();
        task.execute();
    }
    public void callQuery(View view)
    {
        CallQueryAsyncTask task = new CallQueryAsyncTask();
        task.execute();
    }

    private class PutServerAsyncTask extends AsyncTask<String, Void, String> {
        protected String doInBackground(String... urls) {
            String sendKey = ((EditText)findViewById(R.id.key_to_server)).getText().toString();
            String sendValue = ((EditText)findViewById(R.id.value_to_server)).getText().toString();
            JSONObject jsonObject = new JSONObject();

            String url = String.format("%s/Thingworx/Things/%s/Properties/*", baseURl, thing); //Add wildcard to request for manipulation
            try {
                jsonObject.put(sendKey, sendValue);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            RequestBody body = RequestBody.create(jsonObject.toString(), JSON); //payLoad
            Log.d("MarcelNEEDScheiße", jsonObject.toString());
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Content-Type", "application/json")
                    .addHeader("appkey", appKey)
                    .put(body)
                    .build();
            Log.d("ThingWorxActivity", request.toString());
            String responseStr = "";
            try  {
                Response response = client.newCall(request).execute();
                //responseStr =  response.body().string();
                responseStr =  response.toString();
                Log.d("ThingWorxActivity", responseStr);
            } catch (IOException e)
            {
                Log.e("ThingWorxActivity", String.valueOf(e));
            }
            return responseStr;
        }
        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(String result) {
            TextView feedback = findViewById(R.id.to_server_feedback);
            feedback.setText(result);
        }
    }

    private class GetServerAsyncTask extends AsyncTask<String, Void, String> {
        protected String doInBackground(String... urls) {
            String sendKey = ((EditText)findViewById(R.id.key_from_server)).getText().toString();
            String url = String.format("%s/Thingworx/Things/%s/Properties/%s", baseURl, thing, sendKey); //Add the parameter to the url to fetch value
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Accept", "application/json")
                    .addHeader("appkey", appKey)
                    .get()
                    .build();
            Log.d("ThingWorxActivity", request.toString());
            String responseStr = "";
            try  {
                Response response = client.newCall(request).execute();
                //responseStr =  response.body().string();
                responseStr =  response.body().string();
                Log.d("ThingWorxActivity", responseStr);


                JsonObject obj = new Gson().fromJson(responseStr, JsonObject.class);

                responseStr = obj.getAsJsonArray("rows").get(0).getAsJsonObject().get(sendKey).getAsString();
            } catch (Exception e)
            {
                Log.e("ThingWorxActivity", String.valueOf(e));
            }

            return responseStr;
        }
        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(String result) {
            TextView feedback = findViewById(R.id.from_server_value);
            feedback.setText(result);
        }
    }


    private class CallServiceAsyncTask extends AsyncTask<String, Void, String> {
        /* This is how the JSON request has to look like:
        {
    "values": {
        "dataShape": {
            "fieldDefinitions": {
                "par1": {
                    "name": "par1",
                    "baseType": "STRING",
                },
                "par2": {
                    "name": "par2",
                    "baseType": "NUMBER",
                }
            }
        },
        "rows": [{
                "par1": "new10",
                "par2": 100
            }
        ]
    }
}
         */
        private JSONObject generateDataJSON() throws JSONException
        {
            JSONObject fieldDefs = new JSONObject();
            JSONObject fieldDefsContent = new JSONObject();
            fieldDefsContent.put("key", generateFieldDefinition("key", "STRING"));
            fieldDefsContent.put("sensor", generateFieldDefinition("sensor", "JSON"));
            fieldDefs.put("fieldDefinitions", fieldDefsContent);

            JSONObject dataShape = new JSONObject();
            dataShape.put("dataShape", fieldDefs);

            JSONObject values = new JSONObject();
            values.put("values", dataShape);

            JSONArray rows = new JSONArray();
            rows.put(new JSONObject()
                    .put("key", "PJenewein")
                    .put("sensor", listAllSensors()));
            dataShape.put("rows", rows);
            return values;
        }

        private JSONObject generateFieldDefinition(String name, String baseType) throws JSONException
        {

            JSONObject def = new JSONObject();
            def.put("name", name);
            def.put("baseType", baseType);
            return def;
        }

        protected String doInBackground(String... urls) {
            String url = String.format("%s/Thingworx/Things/%s/Services/AddOrUpdateDataTableEntry", baseURl, datatable); //Add the parameter to the url to fetch value
            RequestBody body = null;
            try {
                String jsonStr = generateDataJSON().toString();
                Log.d("ThingWorxActivity", jsonStr);
                body = RequestBody.create(jsonStr, JSON);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Accept", "application/json")
                    .addHeader("appkey", appKey)
                    .post(body)
                    .build();
            Log.d("ThingWorxActivity", request.toString());
            String responseStr = "";
            try  {
                Response response = client.newCall(request).execute();
                //responseStr =  response.body().string();
                responseStr =  response.body().string();
                Log.d("ThingWorxActivity", responseStr);


                JsonObject obj = new Gson().fromJson(responseStr, JsonObject.class);

                //responseStr = obj.getAsJsonArray("rows").get(0).getAsJsonObject().get(sendKey).getAsString();
            } catch (Exception e)
            {
                Log.e("ThingWorxActivity", String.valueOf(e));
            }

            return responseStr;
        }

        /**
         * Used to public progress, just call publishProgress((int) from doInBackground
         * @param values
         */
        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(String result) {
            TextView feedback = findViewById(R.id.service_result);
            feedback.setText(result);
        }
    }

    private class CallQueryAsyncTask extends AsyncTask<String, Void, String> {
        /* This is how the JSON request has to look like:
 {"query" :{"filters":
        {
            "type": "LT",
            "fieldName": "par2",
            "value": "20"
        }
      }
   }
         */
        private JSONObject generateDataJSON() throws JSONException
        {
            JSONObject query = new JSONObject().put("filters", new JSONObject()
                    .put("type", "EQ")
                    .put("fieldName", "key")
                    .put("value", "PJenewein"));

            return new JSONObject().put("query",query);
        }

        private JSONObject generateQueryDefinition(String name, String baseType) throws JSONException
        {

            JSONObject def = new JSONObject();
            def.put("name", name);
            def.put("baseType", baseType);
            return def;
        }

        protected String doInBackground(String... urls) {
            String sendKey = ((EditText)findViewById(R.id.key_from_server)).getText().toString();
            String url = String.format("%s/Thingworx/Things/%s/Services/QueryDataTableEntries", baseURl, datatable, sendKey); //Add the parameter to the url to fetch value
            RequestBody body = null;
            try {
                String jsonStr = generateDataJSON().toString();
                Log.d("ThingWorxActivity", jsonStr);
                body = RequestBody.create(jsonStr, JSON);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("Accept", "application/json")
                    .addHeader("appkey", appKey)
                    .post(body)
                    .build();
            Log.d("ThingWorxActivity", request.toString());
            String responseStr = "";
            StringBuilder res = new StringBuilder();
            try  {
                Response response = client.newCall(request).execute();
                responseStr =  response.body().string();
                Log.d("ThingWorxActivity", responseStr);


                JsonObject obj = new Gson().fromJson(responseStr, JsonObject.class);

                JsonArray rows = obj.getAsJsonArray("rows");

                for (JsonElement row : rows)
                {
                    JsonObject rowO = row.getAsJsonObject();
                    res.append(String.format("key: %s sensor: %s\n", rowO.get("key"), rowO.get("sensor")));
                }
            } catch (Exception e)
            {
                Log.e("ThingWorxActivity", String.valueOf(e));
            }
            return res.toString();
        }
        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(String result) {
            TextView feedback = findViewById(R.id.service_result);
            feedback.setText(result);
        }
    }

    }
