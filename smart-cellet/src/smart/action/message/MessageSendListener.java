package smart.action.message;

import java.nio.charset.Charset;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import net.cellcloud.common.Logger;
import net.cellcloud.core.Cellet;
import net.cellcloud.talk.dialect.ActionDialect;
import net.cellcloud.util.ObjectProperty;
import net.cellcloud.util.Properties;

import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.DeferredContentProvider;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.json.JSONException;
import org.json.JSONObject;

import smart.action.AbstractListener;
import smart.api.API;
import smart.api.RequestContentCapsule;
import smart.api.host.HostConfig;
import smart.api.host.HostConfigContext;
import smart.api.host.ServiceDeskHostConfig;
import smart.mast.action.Action;

/**
 * 发送消息监听器
 * 
 */
public final class MessageSendListener extends AbstractListener {

	public MessageSendListener(Cellet cellet) {
		super(cellet);
	}

	@Override
	public void onAction(ActionDialect action) {

		// 使用同步方式进行请求
		// 注意：因为onAction是由cell cloud 的action dialect进行回调的
		// 该方法独享一个线程，因此可以在此线程里进行阻塞式调用
		// 因此，在这里可以用同步的方式请求HTTP API

		// 获取参数
		JSONObject json = null;
		String title = null;
		String sender = null;
		String recieverIds = null;
		String time = null;
		String contents = null;
		try {
			json = new JSONObject(action.getParamAsString("data"));
			title = json.getString("title");
			sender = json.getString("sender");
			recieverIds = json.getString("recieverIds");
			time = json.getString("time");
			contents = json.getString("contents");
		} catch (JSONException e1) {
			e1.printStackTrace();
		}

		System.out.println(recieverIds);
		System.out.println("title    " + title);
		System.out.println("time    " + time);
		System.out.println("contents    " + contents);
		// URL
		HostConfig config = new ServiceDeskHostConfig();
		HostConfigContext context = new HostConfigContext(config);
		StringBuilder url = new StringBuilder(context.getAPIHost()).append("/")
				.append(API.MESSAGESEND);

		url.append("&title=").append(title).append("&sender=").append(sender)
				.append("&content=").append(contents).append("&recieverIds=")
				.append(recieverIds);
		System.out.println("url   " + url);

		// 创建请求
		Request request = this.getHttpClient().newRequest(url.toString());
		request.method(HttpMethod.POST);
		url = null;

		// 填写数据内容
		DeferredContentProvider dcp = new DeferredContentProvider();

		RequestContentCapsule capsule = new RequestContentCapsule();
		capsule.append("title", title);
		capsule.append("recieverIds", recieverIds);
		capsule.append("sender", sender);
		capsule.append("content", contents);
		capsule.append("time", time);
		dcp.offer(capsule.toBuffer());
		dcp.close();
		request.content(dcp);

		// 发送请求
		ContentResponse response = null;
		try {
			response = request.send();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		} catch (TimeoutException e1) {
			e1.printStackTrace();
		} catch (ExecutionException e1) {
			e1.printStackTrace();
		}

		Properties params = new Properties();
		JSONObject data = null;
		switch (response.getStatus()) {
		case HttpStatus.OK_200:
			byte[] bytes = response.getContent();
			if (null != bytes) {

				// 获取从Web服务器上返回的数据
				String content = new String(bytes, Charset.forName("UTF-8"));

				System.out.println("源数据-messageDetail：" + content);
				try {
					data = new JSONObject(content);

					if (data.get("success").equals(true)) {
						// 设置参数
						params.addProperty(new ObjectProperty("data", data));
					} else {
						data.remove("root");
						data.put("root", "");
						data.put("status", 412);
						data.put("errorInfo", "未获取到消息数据");
						params.addProperty(new ObjectProperty("data", data));
					}
					System.out.println("send  " + data);
				} catch (JSONException e) {
					e.printStackTrace();
				}

				// 响应动作，即想客户端发送ActionDialect
				// 参数tracker 是一次动作的追踪表示。
				this.response(Action.MESSAGEDETAIL, params);

			} else {
				this.reportHTTPError(Action.MESSAGESEND);
			}
			break;
		default:
			Logger.w(MessageSendListener.class, "返回响应码" + response.getStatus());
			try {
				data = new JSONObject();
				data.put("status", 900);
			} catch (JSONException e) {
				e.printStackTrace();
			}

			// 设置参数
			params.addProperty(new ObjectProperty("data", data));
			// 响应动作，即向客户端发送 ActionDialect
			this.response(Action.MESSAGESEND, params);
			break;
		}

	}
}
