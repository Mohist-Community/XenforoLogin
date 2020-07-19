/*
 * This file is part of XenforoLogin, licensed under the GNU Lesser General Public License v3.0 (LGPLv3).
 *
 * You are not permitted to interfere any protection that prevents loading in CatServer
 *
 * Copyright (c) 2020 Mohist-Community.
 *
 */

package red.mohist.xenforologin.core.proxys.implementations;

import org.apache.http.HttpEntity;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.fluent.Request;
import org.apache.http.util.EntityUtils;
import red.mohist.xenforologin.core.proxys.ProxySystem;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QiYun implements ProxySystem
{
    private CopyOnWriteArrayList<String> proxyList;
    public QiYun(){
        proxyList = new CopyOnWriteArrayList<>();
    }

    public String getName() {
        return "89ip";
    }

    public void refreshProxys() throws IOException {
        ResponseHandler<String> responseHandler = response -> {
            int status = response.getStatusLine().getStatusCode();
            if (status == 200) {
                HttpEntity entity = response.getEntity();
                return entity != null ? EntityUtils.toString(entity) : null;
            }
            return null;
        };

        String result = Request.Get("http://www.89ip.cn/tqdl.html?api=1&num=9999")
                .addHeader("accept", "*/*")
                .addHeader("connection", "Keep-Alive")
                .addHeader("user-agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1;SV1)")
                .execute().handleResponse(responseHandler);
        if(result == null){
            throw new IOException("Result is null");
        }
        Pattern pattern = Pattern.compile("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
        Matcher matcher = pattern.matcher(result);
        while (matcher.find()) {
            String ip = matcher.group();
            if (!proxyList.contains(ip)) {
                proxyList.add(ip);
            }
        }
    }

    public boolean isProxy(String ip){
        return proxyList.contains(ip);
    }
}