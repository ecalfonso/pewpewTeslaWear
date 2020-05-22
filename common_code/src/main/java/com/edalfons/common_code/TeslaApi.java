package com.edalfons.common_code;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class TeslaApi {
    private final String access_token;
    private final String id_s;

    public int respCode = HttpURLConnection.HTTP_UNAUTHORIZED;
    public JSONObject resp = null;

    private final String client_id = "81527cff06843c8634fdc09e8ac0abefb46ac849f38fe1e431c2ef2106796384";
    private final String client_secret = "c7257eb71a564034f9419ee651c7d0e5f7aa6bfbd18bafb5c5c033b093bb2fa3";

    public TeslaApi(String aToken, String id_s) {
        this.access_token = aToken;
        this.id_s = id_s;
    }

    public TeslaApi(String aToken) {
        this.access_token = aToken;
        this.id_s = "";
    }

    public TeslaApi() {
        this.access_token = "";
        this.id_s = "";
    }

    private void setRespCode(int rCode) {
        respCode = rCode;
    }

    private void setResp(InputStream in) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        StringBuilder buffer = new StringBuilder();
        String line;

        try {
            while ((line = reader.readLine()) != null) {
                buffer.append(line).append('\n');
            }

            resp = new JSONObject(buffer.toString());

            reader.close();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }

    public void reset() {
        this.respCode = HttpURLConnection.HTTP_UNAUTHORIZED;
        this.resp = null;
    }

    public void getApiStatus() {
        HttpURLConnection httpConn = null;

        try {
            URL url = new URL("https://owner-api.teslamotors.com/status/");
            httpConn = (HttpURLConnection) url.openConnection();

            respCode = httpConn.getResponseCode();

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (httpConn != null) {
                httpConn.disconnect();
            }
        }
    }

    public void login(String username, String password) {
        HttpURLConnection httpConn = null;
        JSONObject jsonBody = new JSONObject();

        try {
            URL url = new URL("https://owner-api.teslamotors.com/oauth/token");
            httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setRequestMethod("POST");
            httpConn.setRequestProperty("Content-type", "application/json");

            /* Build JSON body with username/password */
            jsonBody.put("grant_type", "password");
            jsonBody.put("client_id", client_id);
            jsonBody.put("client_secret", client_secret);
            jsonBody.put("email", username);
            jsonBody.put("password", password);

            /* Attach JSON body to POST request */
            OutputStream os = httpConn.getOutputStream();
            os.write(jsonBody.toString().getBytes(StandardCharsets.UTF_8));
            os.close();

            httpConn.connect();

            this.setRespCode(httpConn.getResponseCode());
            this.setResp(httpConn.getInputStream());
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        } finally {
            if (httpConn != null) {
                httpConn.disconnect();
            }
        }
    }

    public void refreshToken(String refresh_token) {
        HttpURLConnection httpConn = null;
        JSONObject jsonBody = new JSONObject();

        try {
            URL url = new URL("https://owner-api.teslamotors.com/oauth/token");
            httpConn = (HttpURLConnection)url.openConnection();
            httpConn.setRequestMethod("POST");
            httpConn.setRequestProperty("Content-type", "application/json");

            /* Build JSON body */
            jsonBody.put("grant_type", "refresh_token");
            jsonBody.put("client_id", client_id);
            jsonBody.put("client_secret", client_secret);
            jsonBody.put("refresh_token", refresh_token);

            /* Attach JSON body to POST request */
            OutputStream os = httpConn.getOutputStream();
            os.write(jsonBody.toString().getBytes(StandardCharsets.UTF_8));
            os.close();

            httpConn.connect();

            this.setRespCode(httpConn.getResponseCode());
            this.setResp(httpConn.getInputStream());
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        } finally {
            if (httpConn != null) {
                httpConn.disconnect();
            }
        }
    }

    public void getVehicleList() {
        HttpURLConnection httpConn = null;

        try {
            URL url = new URL("https://owner-api.teslamotors.com/api/1/vehicles");
            httpConn = (HttpURLConnection)url.openConnection();
            httpConn.setRequestProperty("Content-type", "application/json");
            httpConn.setRequestProperty("Authorization", "Bearer ".concat(access_token));
            httpConn.connect();

            this.setRespCode(httpConn.getResponseCode());
            this.setResp(httpConn.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (httpConn != null) {
                httpConn.disconnect();
            }
        }
    }

    public String getVehicleWakeStatusFromVehicleList() {
        int i;

        try {
            this.reset();
            this.getVehicleList();

            for (i = 0; i < resp.getInt("count"); i++) {
                if (resp.getJSONArray("response").getJSONObject(i)
                        .getString("id_s").matches(id_s)) {
                    return resp.getJSONArray("response").getJSONObject(i)
                            .getString("state");
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return "asleep";
    }

    public void getVehicleData() {
        HttpURLConnection httpConn = null;

        try {
            URL url = new URL("https://owner-api.teslamotors.com/api/1/vehicles/" +
                    id_s + "/vehicle_data");
            httpConn = (HttpURLConnection)url.openConnection();
            httpConn.setRequestProperty("Content-type", "application/json");
            httpConn.setRequestProperty("Authorization", "Bearer ".concat(access_token));
            httpConn.connect();

            this.setRespCode(httpConn.getResponseCode());
            this.setResp(httpConn.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (httpConn != null) {
                httpConn.disconnect();
            }
        }
    }

    private void wakeupVehicle() {
        HttpURLConnection httpConn = null;

        try {
            URL url = new URL("https://owner-api.teslamotors.com/api/1/vehicles/" +
                    id_s + "/wake_up");
            httpConn = (HttpURLConnection)url.openConnection();
            httpConn.setRequestMethod("POST");
            httpConn.setRequestProperty("Content-type", "application/json");
            httpConn.setRequestProperty("Authorization", "Bearer ".concat(access_token));
            httpConn.connect();

            this.setRespCode(httpConn.getResponseCode());
            this.setResp(httpConn.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (httpConn != null) {
                httpConn.disconnect();
            }
        }
    }

    public void waitUntilVehicleAwake() {
        int i;

        try {
            for (i = 0; i < 12; i++) {
                this.reset();
                this.wakeupVehicle();

                if (this.getVehicleWakeStatusFromVehicleList().matches("online")) {
                    break;
                }

                Thread.sleep(5000);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void unlockVehicle() {
        HttpURLConnection httpConn = null;

        try {
            this.reset();
            this.waitUntilVehicleAwake();

            URL url = new URL("https://owner-api.teslamotors.com/api/1/vehicles/" +
                    id_s + "/command/door_unlock");
            httpConn = (HttpURLConnection)url.openConnection();
            httpConn.setRequestMethod("POST");
            httpConn.setRequestProperty("Content-type", "application/json");
            httpConn.setRequestProperty("Authorization", "Bearer ".concat(access_token));
            httpConn.connect();

            this.setRespCode(httpConn.getResponseCode());
            this.setResp(httpConn.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (httpConn != null) {
                httpConn.disconnect();
            }
        }
    }

    public void lockVehicle() {
        HttpURLConnection httpConn = null;

        try {
            this.reset();
            this.waitUntilVehicleAwake();

            URL url = new URL("https://owner-api.teslamotors.com/api/1/vehicles/" +
                    id_s + "/command/door_lock");
            httpConn = (HttpURLConnection)url.openConnection();
            httpConn.setRequestMethod("POST");
            httpConn.setRequestProperty("Content-type", "application/json");
            httpConn.setRequestProperty("Authorization", "Bearer ".concat(access_token));
            httpConn.connect();

            this.setRespCode(httpConn.getResponseCode());
            this.setResp(httpConn.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (httpConn != null) {
                httpConn.disconnect();
            }
        }
    }

    public void closeVehicleWindows() {
        HttpURLConnection httpConn = null;
        JSONObject jsonBody = new JSONObject();

        try {
            this.reset();
            this.waitUntilVehicleAwake();

            URL url = new URL("https://owner-api.teslamotors.com/api/1/vehicles/" +
                    id_s + "/command/window_control");
            httpConn = (HttpURLConnection)url.openConnection();
            httpConn.setRequestMethod("POST");
            httpConn.setRequestProperty("Content-type", "application/json");
            httpConn.setRequestProperty("Authorization", "Bearer ".concat(access_token));

            /* Build JSON body */
            jsonBody.put("command", "close");
            jsonBody.put("lat", 0);
            jsonBody.put("lon", 0);

            /* Attach JSON body to POST request */
            OutputStream os = httpConn.getOutputStream();
            os.write(jsonBody.toString().getBytes(StandardCharsets.UTF_8));
            os.close();

            httpConn.connect();

            this.setRespCode(httpConn.getResponseCode());
            this.setResp(httpConn.getInputStream());
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        } finally {
            if (httpConn != null) {
                httpConn.disconnect();
            }
        }
    }

    public void ventVehicleWindows() {
        HttpURLConnection httpConn = null;
        JSONObject jsonBody = new JSONObject();

        try {
            this.reset();
            this.waitUntilVehicleAwake();

            URL url = new URL("https://owner-api.teslamotors.com/api/1/vehicles/" +
                    id_s + "/command/window_control");
            httpConn = (HttpURLConnection)url.openConnection();
            httpConn.setRequestMethod("POST");
            httpConn.setRequestProperty("Content-type", "application/json");
            httpConn.setRequestProperty("Authorization", "Bearer ".concat(access_token));

            /* Build JSON body */
            jsonBody.put("command", "vent");
            jsonBody.put("lat", 0);
            jsonBody.put("lon", 0);

            /* Attach JSON body to POST request */
            OutputStream os = httpConn.getOutputStream();
            os.write(jsonBody.toString().getBytes(StandardCharsets.UTF_8));
            os.close();

            httpConn.connect();

            this.setRespCode(httpConn.getResponseCode());
            this.setResp(httpConn.getInputStream());
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        } finally {
            if (httpConn != null) {
                httpConn.disconnect();
            }
        }
    }

    public void actuateFrunk() {
        HttpURLConnection httpConn = null;
        JSONObject jsonBody = new JSONObject();

        try {
            this.reset();
            this.waitUntilVehicleAwake();

            URL url = new URL("https://owner-api.teslamotors.com/api/1/vehicles/" +
                    id_s + "/command/actuate_trunk");
            httpConn = (HttpURLConnection)url.openConnection();
            httpConn.setRequestMethod("POST");
            httpConn.setRequestProperty("Content-type", "application/json");
            httpConn.setRequestProperty("Authorization", "Bearer ".concat(access_token));

            /* Build JSON body */
            jsonBody.put("which_trunk", "front");

            /* Attach JSON body to POST request */
            OutputStream os = httpConn.getOutputStream();
            os.write(jsonBody.toString().getBytes(StandardCharsets.UTF_8));
            os.close();

            httpConn.connect();

            this.setRespCode(httpConn.getResponseCode());
            this.setResp(httpConn.getInputStream());
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        } finally {
            if (httpConn != null) {
                httpConn.disconnect();
            }
        }
    }

    public void actuateTrunk() {
        HttpURLConnection httpConn = null;
        JSONObject jsonBody = new JSONObject();

        try {
            this.reset();
            this.waitUntilVehicleAwake();

            URL url = new URL("https://owner-api.teslamotors.com/api/1/vehicles/" +
                    id_s + "/command/actuate_trunk");
            httpConn = (HttpURLConnection)url.openConnection();
            httpConn.setRequestMethod("POST");
            httpConn.setRequestProperty("Content-type", "application/json");
            httpConn.setRequestProperty("Authorization", "Bearer ".concat(access_token));

            /* Build JSON body */
            jsonBody.put("which_trunk", "rear");

            /* Attach JSON body to POST request */
            OutputStream os = httpConn.getOutputStream();
            os.write(jsonBody.toString().getBytes(StandardCharsets.UTF_8));
            os.close();

            httpConn.connect();

            this.setRespCode(httpConn.getResponseCode());
            this.setResp(httpConn.getInputStream());
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        } finally {
            if (httpConn != null) {
                httpConn.disconnect();
            }
        }
    }

    public void setChargeLimit(int limit) {
        HttpURLConnection httpConn = null;
        JSONObject jsonBody = new JSONObject();

        try {
            this.reset();
            this.waitUntilVehicleAwake();

            URL url = new URL("https://owner-api.teslamotors.com/api/1/vehicles/" +
                    id_s + "/command/set_charge_limit");
            httpConn = (HttpURLConnection)url.openConnection();
            httpConn.setRequestMethod("POST");
            httpConn.setRequestProperty("Content-type", "application/json");
            httpConn.setRequestProperty("Authorization", "Bearer ".concat(access_token));

            /* Build JSON body */
            jsonBody.put("percent", limit);

            /* Attach JSON body to POST request */
            OutputStream os = httpConn.getOutputStream();
            os.write(jsonBody.toString().getBytes(StandardCharsets.UTF_8));
            os.close();

            httpConn.connect();

            this.setRespCode(httpConn.getResponseCode());
            this.setResp(httpConn.getInputStream());
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        } finally {
            if (httpConn != null) {
                httpConn.disconnect();
            }
        }
    }

    public void triggerHomelink() {
        HttpURLConnection httpConn = null;
        JSONObject jsonBody = new JSONObject();

        double lat, lon;

        try {
            this.reset();
            this.waitUntilVehicleAwake();

            /* get car's initial lat/lon */
            this.reset();
            this.getVehicleData();
            lat = this.resp.getJSONObject("response").getJSONObject("drive_state").getDouble("latitude");
            lon = this.resp.getJSONObject("response").getJSONObject("drive_state").getDouble("longitude");

            this.reset();

            URL url = new URL("https://owner-api.teslamotors.com/api/1/vehicles/" +
                    id_s + "/command/trigger_homelink");
            httpConn = (HttpURLConnection)url.openConnection();
            httpConn.setRequestMethod("POST");
            httpConn.setRequestProperty("Content-type", "application/json");
            httpConn.setRequestProperty("Authorization", "Bearer ".concat(access_token));

            /* Build JSON body */
            jsonBody.put("lat", lat);
            jsonBody.put("lon", lon);

            /* Attach JSON body to POST request */
            OutputStream os = httpConn.getOutputStream();
            os.write(jsonBody.toString().getBytes(StandardCharsets.UTF_8));
            os.close();

            httpConn.connect();

            this.setRespCode(httpConn.getResponseCode());
            this.setResp(httpConn.getInputStream());
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        } finally {
            if (httpConn != null) {
                httpConn.disconnect();
            }
        }
    }

    public void closeChargePort() {
        HttpURLConnection httpConn = null;

        try {
            this.reset();
            this.waitUntilVehicleAwake();

            URL url = new URL("https://owner-api.teslamotors.com/api/1/vehicles/" +
                    id_s + "/command/charge_port_door_close");
            httpConn = (HttpURLConnection)url.openConnection();
            httpConn.setRequestMethod("POST");
            httpConn.setRequestProperty("Content-type", "application/json");
            httpConn.setRequestProperty("Authorization", "Bearer ".concat(access_token));

            httpConn.connect();

            this.setRespCode(httpConn.getResponseCode());
            this.setResp(httpConn.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (httpConn != null) {
                httpConn.disconnect();
            }
        }
    }

    public void openChargePort() {
        HttpURLConnection httpConn = null;

        try {
            this.reset();
            this.waitUntilVehicleAwake();

            URL url = new URL("https://owner-api.teslamotors.com/api/1/vehicles/" +
                    id_s + "/command/charge_port_door_open");
            httpConn = (HttpURLConnection)url.openConnection();
            httpConn.setRequestMethod("POST");
            httpConn.setRequestProperty("Content-type", "application/json");
            httpConn.setRequestProperty("Authorization", "Bearer ".concat(access_token));

            httpConn.connect();

            this.setRespCode(httpConn.getResponseCode());
            this.setResp(httpConn.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (httpConn != null) {
                httpConn.disconnect();
            }
        }
    }

    public void startCharging() {
        HttpURLConnection httpConn = null;

        try {
            this.reset();
            this.waitUntilVehicleAwake();

            URL url = new URL("https://owner-api.teslamotors.com/api/1/vehicles/" +
                    id_s + "/command/charge_start");
            httpConn = (HttpURLConnection)url.openConnection();
            httpConn.setRequestMethod("POST");
            httpConn.setRequestProperty("Content-type", "application/json");
            httpConn.setRequestProperty("Authorization", "Bearer ".concat(access_token));

            httpConn.connect();

            this.setRespCode(httpConn.getResponseCode());
            this.setResp(httpConn.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (httpConn != null) {
                httpConn.disconnect();
            }
        }
    }

    public void stopCharging() {
        HttpURLConnection httpConn = null;

        try {
            this.reset();
            this.waitUntilVehicleAwake();

            URL url = new URL("https://owner-api.teslamotors.com/api/1/vehicles/" +
                    id_s + "/command/charge_stop");
            httpConn = (HttpURLConnection)url.openConnection();
            httpConn.setRequestMethod("POST");
            httpConn.setRequestProperty("Content-type", "application/json");
            httpConn.setRequestProperty("Authorization", "Bearer ".concat(access_token));

            httpConn.connect();

            this.setRespCode(httpConn.getResponseCode());
            this.setResp(httpConn.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (httpConn != null) {
                httpConn.disconnect();
            }
        }
    }

    public void sentryModeOn() {
        HttpURLConnection httpConn = null;
        JSONObject jsonBody = new JSONObject();

        try {
            this.reset();
            this.waitUntilVehicleAwake();

            URL url = new URL("https://owner-api.teslamotors.com/api/1/vehicles/" +
                    id_s + "/command/set_sentry_mode");
            httpConn = (HttpURLConnection)url.openConnection();
            httpConn.setRequestMethod("POST");
            httpConn.setRequestProperty("Content-type", "application/json");
            httpConn.setRequestProperty("Authorization", "Bearer ".concat(access_token));

            /* Build JSON body */
            jsonBody.put("on", "true");

            /* Attach JSON body to POST request */
            OutputStream os = httpConn.getOutputStream();
            os.write(jsonBody.toString().getBytes(StandardCharsets.UTF_8));
            os.close();

            httpConn.connect();

            this.setRespCode(httpConn.getResponseCode());
            this.setResp(httpConn.getInputStream());
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        } finally {
            if (httpConn != null) {
                httpConn.disconnect();
            }
        }
    }

    public void sentryModeOff() {
        HttpURLConnection httpConn = null;
        JSONObject jsonBody = new JSONObject();

        try {
            this.reset();
            this.waitUntilVehicleAwake();

            URL url = new URL("https://owner-api.teslamotors.com/api/1/vehicles/" +
                    id_s + "/command/set_sentry_mode");
            httpConn = (HttpURLConnection)url.openConnection();
            httpConn.setRequestMethod("POST");
            httpConn.setRequestProperty("Content-type", "application/json");
            httpConn.setRequestProperty("Authorization", "Bearer ".concat(access_token));

            /* Build JSON body */
            jsonBody.put("on", "false");

            /* Attach JSON body to POST request */
            OutputStream os = httpConn.getOutputStream();
            os.write(jsonBody.toString().getBytes(StandardCharsets.UTF_8));
            os.close();

            httpConn.connect();

            this.setRespCode(httpConn.getResponseCode());
            this.setResp(httpConn.getInputStream());
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        } finally {
            if (httpConn != null) {
                httpConn.disconnect();
            }
        }
    }

    public void startClimate() {
        HttpURLConnection httpConn = null;

        try {
            this.reset();
            this.waitUntilVehicleAwake();

            URL url = new URL("https://owner-api.teslamotors.com/api/1/vehicles/" +
                    id_s + "/command/auto_conditioning_start");
            httpConn = (HttpURLConnection)url.openConnection();
            httpConn.setRequestMethod("POST");
            httpConn.setRequestProperty("Content-type", "application/json");
            httpConn.setRequestProperty("Authorization", "Bearer ".concat(access_token));

            httpConn.connect();

            this.setRespCode(httpConn.getResponseCode());
            this.setResp(httpConn.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (httpConn != null) {
                httpConn.disconnect();
            }
        }
    }

    public void stopClimate() {
        HttpURLConnection httpConn = null;

        try {
            this.reset();
            this.waitUntilVehicleAwake();

            URL url = new URL("https://owner-api.teslamotors.com/api/1/vehicles/" +
                    id_s + "/command/auto_conditioning_stop");
            httpConn = (HttpURLConnection)url.openConnection();
            httpConn.setRequestMethod("POST");
            httpConn.setRequestProperty("Content-type", "application/json");
            httpConn.setRequestProperty("Authorization", "Bearer ".concat(access_token));

            httpConn.connect();

            this.setRespCode(httpConn.getResponseCode());
            this.setResp(httpConn.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (httpConn != null) {
                httpConn.disconnect();
            }
        }
    }
}

