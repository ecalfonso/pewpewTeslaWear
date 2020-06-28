package com.edalfons.common_code;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.os.SystemClock;

import androidx.core.app.NotificationCompat;

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
import java.util.Locale;

@SuppressWarnings("unused")
public class TeslaApi {
    /* Notification Channel IDs */
    private final static String COMMAND_PASS_CHANNEL_ID = "cmd_pass_channel_id";
    private final static String COMMAND_FAIL_CHANNEL_ID = "cmd_fail_channel_id";
    private final static String WAKEUP_VEHICLE_CHANNEL_ID = "wakeup_vehicle_channel_id";

    /* Command enumerations */
    public final static int CMD_LOCK = 0;
    public final static int CMD_UNLOCK = 1;
    public final static int CMD_HONK_HORN = 2;
    public final static int CMD_FLASH_LIGHTS = 3;
    public final static int CMD_CLIMATE_ON = 4;
    public final static int CMD_CLIMATE_OFF = 5;
    public final static int CMD_MAX_DEFROST = 6;
    public final static int CMD_SET_TEMPERATURE = 7;
    public final static int CMD_SET_CHARGE_LIMIT = 8;
    public final static int CMD_VENT_WINDOW = 9;
    public final static int CMD_CLOSE_WINDOW = 10;
    public final static int CMD_ACTUATE_FRUNK = 11;
    public final static int CMD_ACTUATE_TRUNK = 12;
    public final static int CMD_REMOTE_START = 13;
    public final static int CMD_HOMELINK = 14;
    public final static int CMD_OPEN_CHARGE_PORT = 15;
    public final static int CMD_CLOSE_CHARGE_PORT = 16;
    public final static int CMD_START_CHARGE = 17;
    public final static int CMD_STOP_CHARGE = 18;
    public final static int CMD_SENTRY_MODE_ON = 19;
    public final static int CMD_SENTRY_MODE_OFF = 20;
    public final static int CMD_SEAT_HEATER = 21;
    public final static int CMD_WHEEL_HEATER = 22;

    /* Private internal variables */
    private final String access_token;
    private final String id_s;
    private final Context ctx;

    /* Private static internal variables */
    private static final String client_id = "81527cff06843c8634fdc09e8ac0abefb46ac849f38fe1e431c2ef2106796384";
    private static final String client_secret = "c7257eb71a564034f9419ee651c7d0e5f7aa6bfbd18bafb5c5c033b093bb2fa3";

    /* Returnable values from HTTP request */
    public int respCode = HttpURLConnection.HTTP_UNAUTHORIZED;
    public JSONObject resp = null;

    /*
     * Public  Constructors
     */
    public TeslaApi(Context c, String aToken, String id_s) {
        this.access_token = aToken;
        this.id_s = id_s;
        this.ctx = c;
    }

    public TeslaApi(String aToken, String id_s) {
        this.access_token = aToken;
        this.id_s = id_s;
        this.ctx = null;
    }

    public TeslaApi(String aToken) {
        this.access_token = aToken;
        this.id_s = "";
        this.ctx = null;
    }

    public TeslaApi() {
        this.access_token = "";
        this.id_s = "";
        this.ctx = null;
    }

    /*
     * Set returnable variables
     */
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

    /*
     * Tesla API Calls
     */
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

    public void getVehicleSummary() {
        HttpURLConnection httpConn = null;

        try {
            URL url = new URL("https://owner-api.teslamotors.com/api/1/vehicles/" + id_s);
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
            int notification_id = (int)SystemClock.uptimeMillis();
            NotificationCompat.Builder builder;
            assert this.ctx != null;
            NotificationManager notificationManager = this.ctx.getSystemService(NotificationManager.class);

            this.reset();
            this.getVehicleSummary();

            if (this.resp.getJSONObject("response")
                    .getString("state")
                    .matches("online")) {
                return;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                /* Register successful notification channel */
                CharSequence name = this.ctx.getString(R.string.notification_channel_vehicle_wakeup_name);
                NotificationChannel channel = new NotificationChannel(WAKEUP_VEHICLE_CHANNEL_ID, name,
                        NotificationManager.IMPORTANCE_LOW);
                assert notificationManager != null;
                notificationManager.createNotificationChannel(channel);
            }

            builder = new NotificationCompat.Builder(this.ctx, WAKEUP_VEHICLE_CHANNEL_ID)
                    .setContentTitle("Waking up vehicle")
                    .setSmallIcon(R.drawable.notification_icon)
                    .setShowWhen(true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                builder.setPriority(NotificationManager.IMPORTANCE_LOW);
            }

            /* Display notification */
            assert notificationManager != null;
            notificationManager.notify(notification_id, builder.build());

            this.reset();
            this.wakeupVehicle();

            for (i = 0; i < 60; i++) {
                this.reset();
                this.getVehicleSummary();

                if (this.resp.getJSONObject("response")
                        .getString("state")
                        .matches("online")) {
                    break;
                }

                Thread.sleep(1000);
            }

            /* Cancel notification */
            notificationManager.cancel(notification_id);
        } catch (InterruptedException | JSONException e) {
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

    public void honkHorn() {
        HttpURLConnection httpConn = null;

        try {
            this.reset();
            this.waitUntilVehicleAwake();

            URL url = new URL("https://owner-api.teslamotors.com/api/1/vehicles/" +
                    id_s + "/command/honk_horn");
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

    public void flashLights() {
        HttpURLConnection httpConn = null;

        try {
            this.reset();
            this.waitUntilVehicleAwake();

            URL url = new URL("https://owner-api.teslamotors.com/api/1/vehicles/" +
                    id_s + "/command/flash_lights");
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

    public void maxDefrost() {
        HttpURLConnection httpConn = null;

        try {
            this.reset();
            this.waitUntilVehicleAwake();

            URL url = new URL("https://owner-api.teslamotors.com/api/1/vehicles/" +
                    id_s + "/command/set_preconditioning_max");
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

    public void setTemps(int driver_temp, int passenger_temp) {
        HttpURLConnection httpConn = null;

        try {
            this.reset();
            this.waitUntilVehicleAwake();

            URL url = new URL("https://owner-api.teslamotors.com/api/1/vehicles/" +
                    id_s + "/command/set_temps?" +
                    "driver_temp=" + driver_temp +
                    "passenger_temp=" + passenger_temp);
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

    public void remoteStart(String password) {
        HttpURLConnection httpConn = null;

        try {
            this.reset();
            this.waitUntilVehicleAwake();

            URL url = new URL("https://owner-api.teslamotors.com/api/1/vehicles/" +
                    id_s + "/command/remote_start_drive?" +
                    "password=" + password);
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

    public void scheduleSoftwareUpdate(int seconds) {
        HttpURLConnection httpConn = null;
        JSONObject jsonBody = new JSONObject();

        try {
            this.reset();
            this.waitUntilVehicleAwake();

            URL url = new URL("https://owner-api.teslamotors.com/api/1/vehicles/" +
                    id_s + "/command/schedule_software_update");
            httpConn = (HttpURLConnection)url.openConnection();
            httpConn.setRequestMethod("POST");
            httpConn.setRequestProperty("Content-type", "application/json");
            httpConn.setRequestProperty("Authorization", "Bearer ".concat(access_token));

            /* Build JSON body */
            jsonBody.put("offset_sec", seconds);

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

    public void startSoftwareUpdate() {
        this.scheduleSoftwareUpdate(0);
    }

    public void cancelSoftwareUpdate() {
        HttpURLConnection httpConn = null;

        try {
            this.reset();
            this.waitUntilVehicleAwake();

            URL url = new URL("https://owner-api.teslamotors.com/api/1/vehicles/" +
                    id_s + "/command/cancel_software_update");
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

    public void seatHeaterRequest(int seat, int level) {
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
            jsonBody.put("heater", seat);
            jsonBody.put("level", level);

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

    public void wheelHeaterOn() {
        HttpURLConnection httpConn = null;
        JSONObject jsonBody = new JSONObject();

        try {
            this.reset();
            this.waitUntilVehicleAwake();

            URL url = new URL("https://owner-api.teslamotors.com/api/1/vehicles/" +
                    id_s + "/command/remote_steering_wheel_heater_request");
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

    public void wheelHeaterOff() {
        HttpURLConnection httpConn = null;
        JSONObject jsonBody = new JSONObject();

        try {
            this.reset();
            this.waitUntilVehicleAwake();

            URL url = new URL("https://owner-api.teslamotors.com/api/1/vehicles/" +
                    id_s + "/command/remote_steering_wheel_heater_request");
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

    /**
     * Thread definitions
     */
    static class TeslaApiThread extends Thread {
        private final TeslaApi teslaApi;
        private final int cmd;
        private final int input;

        TeslaApiThread(TeslaApi t, int c, int i) {
            this.teslaApi = t;
            this.cmd = c;
            this.input = i;
        }

        public void run() {
            if (this.teslaApi.ctx == null) {
                /* Break early if we happen to get here with NULL context */
                return;
            }

            switch (this.cmd) {
                case CMD_LOCK:
                    this.teslaApi.lockVehicle();
                    break;
                case CMD_UNLOCK:
                    this.teslaApi.unlockVehicle();
                    break;
                case CMD_HONK_HORN:
                    /* Nothing implemented */
                    break;
                case CMD_FLASH_LIGHTS:
                    /* Nothing implemented */
                    break;
                case CMD_CLIMATE_ON:
                    this.teslaApi.startClimate();
                    break;
                case CMD_CLIMATE_OFF:
                    this.teslaApi.stopClimate();
                    break;
                case CMD_MAX_DEFROST:
                    /* Nothing implemented */
                    break;
                case CMD_SET_TEMPERATURE:
                    /* Nothing implemented */
                    break;
                case CMD_SET_CHARGE_LIMIT:
                    this.teslaApi.setChargeLimit(this.input);
                    break;
                case CMD_VENT_WINDOW:
                    this.teslaApi.ventVehicleWindows();
                    break;
                case CMD_CLOSE_WINDOW:
                    this.teslaApi.closeVehicleWindows();
                    break;
                case CMD_ACTUATE_FRUNK:
                    this.teslaApi.actuateFrunk();
                    break;
                case CMD_ACTUATE_TRUNK:
                    this.teslaApi.actuateTrunk();
                    break;
                case CMD_REMOTE_START:
                    /* Nothing implemented */
                    break;
                case CMD_HOMELINK:
                    this.teslaApi.triggerHomelink();
                    break;
                case CMD_OPEN_CHARGE_PORT:
                    this.teslaApi.openChargePort();
                    break;
                case CMD_CLOSE_CHARGE_PORT:
                    this.teslaApi.closeChargePort();
                    break;
                case CMD_START_CHARGE:
                    this.teslaApi.startCharging();
                    break;
                case CMD_STOP_CHARGE:
                    this.teslaApi.stopCharging();
                    break;
                case CMD_SENTRY_MODE_ON:
                    this.teslaApi.sentryModeOn();
                    break;
                case CMD_SENTRY_MODE_OFF:
                    this.teslaApi.sentryModeOff();
                    break;
                case CMD_SEAT_HEATER:
                    /* Nothing implemented */
                    break;
                case CMD_WHEEL_HEATER:
                    /* Nothing implemented */
                    break;
                default:
                    /* Unknown cmd */
            }

            NotificationManager notificationManager = this.teslaApi.ctx.getSystemService(NotificationManager.class);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                /* Register successful notification channel */
                CharSequence name = this.teslaApi.ctx.getString(R.string.notification_channel_command_success_name);
                NotificationChannel channel = new NotificationChannel(COMMAND_PASS_CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT);
                assert notificationManager != null;
                notificationManager.createNotificationChannel(channel);

                /* Register successful notification channel */
                name = this.teslaApi.ctx.getString(R.string.notification_channel_command_failure_name);
                channel = new NotificationChannel(COMMAND_FAIL_CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT);
                notificationManager.createNotificationChannel(channel);
            }

            NotificationCompat.Builder builder;

            /* Build success notification */
            try {
                if (this.teslaApi.respCode == HttpURLConnection.HTTP_OK &&
                    this.teslaApi.resp.getJSONObject("response").getBoolean("result")) {
                    builder = new NotificationCompat.Builder(this.teslaApi.ctx, COMMAND_PASS_CHANNEL_ID)
                            .setSmallIcon(R.drawable.notification_icon)
                            .setShowWhen(true);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        builder.setPriority(NotificationManager.IMPORTANCE_DEFAULT);
                    }
                    switch (this.cmd) {
                        case CMD_LOCK:
                            builder.setContentTitle("Locked vehicle");
                            break;
                        case CMD_UNLOCK:
                            builder.setContentTitle("Unlocked vehicle");
                            break;
                        case CMD_HONK_HORN:
                            /* Nothing implemented */
                            break;
                        case CMD_FLASH_LIGHTS:
                            /* Nothing implemented */
                            break;
                        case CMD_CLIMATE_ON:
                            builder.setContentTitle("Turning on climate");
                            break;
                        case CMD_CLIMATE_OFF:
                            builder.setContentTitle("Turning off climate");
                            break;
                        case CMD_MAX_DEFROST:
                            /* Nothing implemented */
                            break;
                        case CMD_SET_TEMPERATURE:
                            /* Nothing implemented */
                            break;
                        case CMD_SET_CHARGE_LIMIT:
                            builder.setContentTitle(String.format(Locale.getDefault(),
                                    "Set charge limit to %d%%", this.input));
                            break;
                        case CMD_VENT_WINDOW:
                            builder.setContentTitle("Venting windows");
                            break;
                        case CMD_CLOSE_WINDOW:
                            builder.setContentTitle("Closing windows");
                            break;
                        case CMD_ACTUATE_FRUNK:
                            builder.setContentTitle("Opening Frunk");
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                builder.setPriority(NotificationManager.IMPORTANCE_HIGH);
                            }
                            break;
                        case CMD_ACTUATE_TRUNK:
                            builder.setContentTitle("Opening Trunk");
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                builder.setPriority(NotificationManager.IMPORTANCE_HIGH);
                            }
                            break;
                        case CMD_REMOTE_START:
                            /* Nothing implemented */
                            break;
                        case CMD_HOMELINK:
                            builder.setContentTitle("Activating Homelink");
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                builder.setPriority(NotificationManager.IMPORTANCE_HIGH);
                            }
                            break;
                        case CMD_OPEN_CHARGE_PORT:
                            builder.setContentTitle("Opening charge port");
                            break;
                        case CMD_CLOSE_CHARGE_PORT:
                            builder.setContentTitle("Closing charge port");
                            break;
                        case CMD_START_CHARGE:
                            builder.setContentTitle("Starting charge");
                            break;
                        case CMD_STOP_CHARGE:
                            builder.setContentTitle("Stopping charge");
                            break;
                        case CMD_SENTRY_MODE_ON:
                            builder.setContentTitle("Turning Sentry Mode on");
                            break;
                        case CMD_SENTRY_MODE_OFF:
                            builder.setContentTitle("Turning Sentry Mode off");
                            break;
                        case CMD_SEAT_HEATER:
                            /* Nothing implemented */
                            break;
                        case CMD_WHEEL_HEATER:
                            /* Nothing implemented */
                            break;
                    }
                }
                /* Build failure notification */
                else {
                    builder = new NotificationCompat.Builder(this.teslaApi.ctx, COMMAND_FAIL_CHANNEL_ID)
                            .setSmallIcon(R.drawable.notification_icon)
                            .setShowWhen(true);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        builder.setPriority(NotificationManager.IMPORTANCE_HIGH);
                    }
                    try {
                        switch (this.cmd) {
                            case CMD_LOCK:
                                builder.setContentTitle("Failed to lock vehicle!")
                                        .setContentText(this.teslaApi.resp.getJSONObject("response")
                                                .getString("reason"));
                                break;
                            case CMD_UNLOCK:
                                builder.setContentTitle("Failed to unlock vehicle!")
                                        .setContentText(this.teslaApi.resp.getJSONObject("response")
                                                .getString("reason"));
                                break;
                            case CMD_HONK_HORN:
                                /* Nothing implemented */
                                break;
                            case CMD_FLASH_LIGHTS:
                                /* Nothing implemented */
                                break;
                            case CMD_CLIMATE_ON:
                                builder.setContentTitle("Failed to turn on climate!")
                                        .setContentText(this.teslaApi.resp.getJSONObject("response")
                                                .getString("reason"));
                                break;
                            case CMD_CLIMATE_OFF:
                                builder.setContentTitle("Failed to turn off climate!")
                                        .setContentText(this.teslaApi.resp.getJSONObject("response")
                                                .getString("reason"));
                                break;
                            case CMD_MAX_DEFROST:
                                /* Nothing implemented */
                                break;
                            case CMD_SET_TEMPERATURE:
                                /* Nothing implemented */
                                break;
                            case CMD_SET_CHARGE_LIMIT:
                                builder.setContentTitle(String.format(Locale.getDefault(),
                                        "Failed to set charge limit to %d%%!", this.input))
                                        .setContentText(this.teslaApi.resp.getJSONObject("response")
                                                .getString("reason"));
                                break;
                            case CMD_VENT_WINDOW:
                                builder.setContentTitle("Failed to vent windows!")
                                        .setContentText(this.teslaApi.resp.getJSONObject("response")
                                                .getString("reason"));
                                break;
                            case CMD_CLOSE_WINDOW:
                                builder.setContentTitle("Failed to close windows!")
                                        .setContentText(this.teslaApi.resp.getJSONObject("response")
                                                .getString("reason"));
                                break;
                            case CMD_ACTUATE_FRUNK:
                                builder.setContentTitle("Failed to open Frunk!")
                                        .setContentText(this.teslaApi.resp.getJSONObject("response")
                                                .getString("reason"));
                                break;
                            case CMD_ACTUATE_TRUNK:
                                builder.setContentTitle("Failed to open Trunk!")
                                        .setContentText(this.teslaApi.resp.getJSONObject("response")
                                                .getString("reason"));
                                break;
                            case CMD_REMOTE_START:
                                /* Nothing implemented */
                                break;
                            case CMD_HOMELINK:
                                builder.setContentTitle("Failed to activate Homelink!")
                                        .setContentText(this.teslaApi.resp.getJSONObject("response")
                                                .getString("reason"));
                                break;
                            case CMD_OPEN_CHARGE_PORT:
                                builder.setContentTitle("Failed to open charge port!")
                                        .setContentText(this.teslaApi.resp.getJSONObject("response")
                                                .getString("reason"));
                                break;
                            case CMD_CLOSE_CHARGE_PORT:
                                builder.setContentTitle("Failed to close charge port!")
                                        .setContentText(this.teslaApi.resp.getJSONObject("response")
                                                .getString("reason"));
                                break;
                            case CMD_START_CHARGE:
                                builder.setContentTitle("Failed to start charge!")
                                        .setContentText(this.teslaApi.resp.getJSONObject("response")
                                                .getString("reason"));
                                break;
                            case CMD_STOP_CHARGE:
                                builder.setContentTitle("Failed to stop charge!")
                                        .setContentText(this.teslaApi.resp.getJSONObject("response")
                                                .getString("reason"));
                                break;
                            case CMD_SENTRY_MODE_ON:
                                builder.setContentTitle("Failed to turn Sentry Mode on!")
                                        .setContentText(this.teslaApi.resp.getJSONObject("response")
                                                .getString("reason"));
                                break;
                            case CMD_SENTRY_MODE_OFF:
                                builder.setContentTitle("Failed to turn Sentry Mode off")
                                        .setContentText(this.teslaApi.resp.getJSONObject("response")
                                                .getString("reason"));
                                break;
                            case CMD_SEAT_HEATER:
                                /* Nothing implemented */
                                break;
                            case CMD_WHEEL_HEATER:
                                /* Nothing implemented */
                                break;
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                /* Display notification */
                assert notificationManager != null;
                notificationManager.notify((int)SystemClock.uptimeMillis(), builder.build());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void sendCmd(int cmd, int input) {
        TeslaApiThread t = new TeslaApiThread(this, cmd, input);
        t.start();
    }
}

