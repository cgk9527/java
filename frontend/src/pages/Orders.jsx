import React, { useEffect, useState } from 'react';
import { Card, List, Tag, Button, Empty, Space, Spin, Modal, Radio, message } from 'antd';
import { QRCodeCanvas } from 'qrcode.react';
import { getOrders, payOrder, cancelOrder } from '../api/index.js';

const STATUS_MAP = {
  0: { text: '待支付', color: 'orange' },
  1: { text: '已支付', color: 'green' },
  2: { text: '已取消', color: 'default' },
  3: { text: '已关闭', color: 'default' }
};

const PAY_CHANNELS = [
  { value: 'mock', label: '模拟支付（本地演示）' },
  { value: 'alipay', label: '支付宝网页支付' },
  { value: 'alipay_qr', label: '支付宝扫码支付' },
  { value: 'wechat', label: '微信扫码支付' }
];

export default function Orders() {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);

  const [payModalOpen, setPayModalOpen] = useState(false);
  const [payOrderId, setPayOrderId] = useState(null);
  const [payChannel, setPayChannel] = useState('alipay');
  const [payResult, setPayResult] = useState(null);
  const [paying, setPaying] = useState(false);

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

  const openPay = (order) => {
    setPayOrderId(order.id);
    setPayChannel('alipay');
    setPayResult(null);
    setPayModalOpen(true);
  };

  const confirmPay = async () => {
    const win = payChannel === 'alipay' ? window.open('about:blank') : null;
    setPaying(true);
    try {
      const res = await payOrder(payOrderId, payChannel);
      if (res.channel === 'mock') {
        message.success('支付成功');
        setPayModalOpen(false);
        load();
      } else if (res.body) {
        if (win) {
          win.document.write(res.body);
          win.document.close();
          message.info('已打开支付宝支付页面，支付完成后刷新本页查看状态');
        } else {
          message.warning('请允许浏览器弹出窗口以完成支付');
        }
        setPayModalOpen(false);
      } else if (res.qrCode) {
        setPayResult(res);
      }
    } finally {
      setPaying(false);
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
                            <Button type="primary" onClick={() => openPay(order)}>立即支付</Button>
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

      <Modal
        title={payResult?.qrCode ? '扫码支付' : '选择支付方式'}
        open={payModalOpen}
        onCancel={() => setPayModalOpen(false)}
        footer={null}
      >
        {payResult?.qrCode ? (
          <div style={{ textAlign: 'center' }}>
            <p>{payResult.channel === 'wechat' ? '请使用微信扫一扫支付' : '请使用支付宝扫一扫支付'}</p>
            <QRCodeCanvas value={payResult.qrCode} size={220} includeMargin />
            <p style={{ color: '#999', marginTop: 8 }}>订单号：{payResult.orderNo}</p>
            <Button
              type="primary"
              onClick={() => { setPayModalOpen(false); message.info('支付结果以订单状态为准'); load(); }}
            >
              我已支付完成
            </Button>
          </div>
        ) : (
          <>
            <Radio.Group
              value={payChannel}
              onChange={(e) => setPayChannel(e.target.value)}
              style={{ display: 'flex', flexDirection: 'column', gap: 10 }}
            >
              {PAY_CHANNELS.map((c) => (
                <Radio key={c.value} value={c.value}>{c.label}</Radio>
              ))}
            </Radio.Group>
            <div style={{ textAlign: 'right', marginTop: 16 }}>
              <Button style={{ marginRight: 8 }} onClick={() => setPayModalOpen(false)}>取消</Button>
              <Button type="primary" loading={paying} onClick={confirmPay}>去支付</Button>
            </div>
          </>
        )}
      </Modal>
    </div>
  );
}