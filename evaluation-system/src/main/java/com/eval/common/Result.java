package com.eval.common;

import java.util.HashMap;
import java.util.Map;

/**
 * 统一响应结果
 */
public class Result {
    private int code;
    private String msg;
    private Object data;

    private Result(int code, String msg, Object data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static Result success() { return new Result(200, "操作成功", null); }
    public static Result success(Object data) { return new Result(200, "操作成功", data); }
    public static Result success(String msg, Object data) { return new Result(200, msg, data); }
    public static Result error(String msg) { return new Result(500, msg, null); }
    public static Result error(int code, String msg) { return new Result(code, msg, null); }

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    public String getMsg() { return msg; }
    public void setMsg(String msg) { this.msg = msg; }
    public Object getData() { return data; }
    public void setData(Object data) { this.data = data; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("code", code);
        map.put("msg", msg);
        map.put("data", data);
        return map;
    }
}
