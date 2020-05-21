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
    public int respCode = HttpURLConnection.HTTP_UNAUTHORIZED;
    public JSONObject resp = null;

    private final String client_id = "81527cff06843c8634fdc09e8ac0abefb46ac849f38fe1e431c2ef2106796384";
    private final String client_secret = "c7257eb71a564034f9419ee651c7d0e5f7aa6bfbd18bafb5c5c033b093bb2fa3";

    public TeslaApi(String aToken) {
        this.access_token = aToken;
    }

    public TeslaApi() {
        this.access_token = "";
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

    public String getVehicleWakeStatusFromVehicleList(String id_s) {
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

    public void getVehicleData(String id_s) {
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

    private void wakeupVehicle(String id_s) {
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

    public void waitUntilVehicleAwake(String id_s) {
        int i;

        try {
            for (i = 0; i < 12; i++) {
                this.reset();
                this.wakeupVehicle(id_s);

                if (this.getVehicleWakeStatusFromVehicleList(id_s).matches("online")) {
                    break;
                }

                Thread.sleep(5000);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void unlockVehicle(String id_s) {
        HttpURLConnection httpConn = null;

        try {
            this.reset();
            this.waitUntilVehicleAwake(id_s);

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

    public void lockVehicle(String id_s) {
        HttpURLConnection httpConn = null;

        try {
            this.reset();
            this.waitUntilVehicleAwake(id_s);

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

    public void closeVehicleWindows(String id_s) {
        HttpURLConnection httpConn = null;
        JSONObject jsonBody = new JSONObject();

        try {
            this.reset();
            this.waitUntilVehicleAwake(id_s);

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

    public void ventVehicleWindows(String id_s) {
        HttpURLConnection httpConn = null;
        JSONObject jsonBody = new JSONObject();

        try {
            this.reset();
            this.waitUntilVehicleAwake(id_s);

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

    public void actuateFrunk(String id_s) {
        HttpURLConnection httpConn = null;
        JSONObject jsonBody = new JSONObject();

        try {
            this.reset();
            this.waitUntilVehicleAwake(id_s);

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

    public void actuateTrunk(String id_s) {
        HttpURLConnection httpConn = null;
        JSONObject jsonBody = new JSONObject();

        try {
            this.reset();
            this.waitUntilVehicleAwake(id_s);

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

    public void setChargeLimit(String id_s, int limit) {
        HttpURLConnection httpConn = null;
        JSONObject jsonBody = new JSONObject();

        try {
            this.reset();
            this.waitUntilVehicleAwake(id_s);

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

    public void triggerHomelink(String id_s) {
        HttpURLConnection httpConn = null;
        JSONObject jsonBody = new JSONObject();

        double lat, lon;

        try {
            this.reset();
            this.waitUntilVehicleAwake(id_s);

            /* get car's initial lat/lon */
            this.reset();
            this.getVehicleData(id_s);
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

    public void closeChargePort(String id_s) {
        HttpURLConnection httpConn = null;

        try {
            this.reset();
            this.waitUntilVehicleAwake(id_s);

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

    public void openChargePort(String id_s) {
        HttpURLConnection httpConn = null;

        try {
            this.reset();
            this.waitUntilVehicleAwake(id_s);

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

    public void startCharging(String id_s) {
        HttpURLConnection httpConn = null;

        try {
            this.reset();
            this.waitUntilVehicleAwake(id_s);

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

    public void stopCharging(String id_s) {
        HttpURLConnection httpConn = null;

        try {
            this.reset();
            this.waitUntilVehicleAwake(id_s);

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

    public void sentryModeOn(String id_s) {
        HttpURLConnection httpConn = null;
        JSONObject jsonBody = new JSONObject();

        try {
            this.reset();
            this.waitUntilVehicleAwake(id_s);

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

    public void sentryModeOff(String id_s) {
        HttpURLConnection httpConn = null;
        JSONObject jsonBody = new JSONObject();

        try {
            this.reset();
            this.waitUntilVehicleAwake(id_s);

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

    public void startClimate(String id_s) {
        HttpURLConnection httpConn = null;

        try {
            this.reset();
            this.waitUntilVehicleAwake(id_s);

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

    public void stopClimate(String id_s) {
        HttpURLConnection httpConn = null;

        try {
            this.reset();
            this.waitUntilVehicleAwake(id_s);

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

