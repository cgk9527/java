import React, { useEffect, useState } from 'react';
import { Card, List, Tag, Button, Empty, Space, Spin, message } from 'antd';
import { getOrders, payOrder, cancelOrder } from '../api/index.js';

const STATUS_MAP = {
  0: { text: '待支付', color: 'orange' },
  1: { text: '已支付', color: 'green' },
  2: { text: '已取消', color: 'default' },
  3: { text: '已关闭', color: 'default' }
};

export default function Orders() {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);

  const load = () => {
    setLoading(true);
    getOrders()
      .then(setOrders)
      .catch(() => {})
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, []);

  const onPay = async (id) => {
    const win = window.open('about:blank');
    const res = await payOrder(id);
    if (res && res.channel === 'alipay' && res.body) {
      if (win) {
        win.document.write(res.body);
        win.document.close();
        message.info('已打开支付宝支付页面，支付完成后刷新本页查看状态');
      } else {
        message.warning('请允许浏览器弹出窗口以完成支付');
      }
    } else {
      if (win) win.close();
      message.success('支付成功');
      load();
    }
  };

  const onCancel = async (id) => {
    await cancelOrder(id);
    message.success('订单已取消');
    load();
  };

  return (
    <div className="page-container">
      <Card title="我的订单">
        <Spin spinning={loading}>
          {orders.length === 0 ? (
            <Empty description="暂无订单" />
          ) : (
            <List
              dataSource={orders}
              renderItem={(order) => {
                const status = STATUS_MAP[order.status] || { text: '未知', color: 'default' };
                return (
                  <List.Item>
                    <div style={{ width: '100%' }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
                        <span style={{ color: '#666' }}>订单号：{order.orderNo}</span>
                        <span style={{ color: '#666' }}>{order.createdAt}</span>
                      </div>
                      {order.items.map((item) => (
                        <div key={item.skuId} style={{ display: 'flex', alignItems: 'center', gap: 16, padding: '8px 0' }}>
                          <img src={item.productImage} alt={item.productName} style={{ width: 50, height: 50, objectFit: 'cover' }} />
                          <div style={{ flex: 1 }}>{item.productName}（{item.skuSpecs}）× {item.quantity}</div>
                          <span className="price">¥{item.totalAmount}</span>
                        </div>
                      ))}
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: 8 }}>
                        <div>
                          <Tag color={status.color}>{status.text}</Tag>
                          <span style={{ marginLeft: 8 }}>合计：<span className="price" style={{ fontSize: 18 }}>¥{order.payAmount}</span></span>
                        </div>
                        {order.status === 0 && (
                          <Space>
                            <Button onClick={() => onCancel(order.id)}>取消订单</Button>
                            <Button type="primary" onClick={() => onPay(order.id)}>立即支付</Button>
                          </Space>
                        )}
                      </div>
                    </div>
                  </List.Item>
                );
              }}
            />
          )}
        </Spin>
      </Card>
    </div>
  );
}