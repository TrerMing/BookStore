package com.itheima.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 购物车订单
 * @author yewmf
 *
 */
public class Orders implements Serializable {
	private String id;
	private String ordernum;
	private double price;
	private int num;
	private int status;
	private Customer c;
	//这个代表一个订单下可以有多个订单项
	private List<OrdersItem> items = new ArrayList<OrdersItem>();
	
	public Orders() {
		super();
	}
	
	public Orders(String ordernum) {
		super();
		this.ordernum = ordernum;
	}

	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getOrdernum() {
		return ordernum;
	}
	public void setOrdernum(String ordernum) {
		this.ordernum = ordernum;
	}
	public double getPrice() {
		return price;
	}
	public void setPrice(double price) {
		this.price = price;
	}
	public int getNum() {
		return num;
	}
	public void setNum(int num) {
		this.num = num;
	}
	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
	}
	public Customer getC() {
		return c;
	}
	public void setC(Customer c) {
		this.c = c;
	}
	public List<OrdersItem> getItems() {
		return items;
	}
	public void setItems(List<OrdersItem> items) {
		this.items = items;
	}

	@Override
	public String toString() {
		return "Orders [id=" + id + ", ordernum=" + ordernum + ", price="
				+ price + ", num=" + num + ", status=" + status + ", c=" + c
				+ ", items=" + items + "]";
	}
	
	
	
}
