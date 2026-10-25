import React, { useEffect, useState } from 'react';
import { Card, Table, Tag, Button, Modal, Input, message, Popconfirm, Space } from 'antd';
import { getAdminOrders, shipOrder } from '../api/index.js';

const STATUS_MAP = {
  0: { text: '待支付', color: 'orange' },
  1: { text: '已支付', color: 'green' },
  2: { text: '已取消', color: 'default' },
  3: { text: '已关闭', color: 'default' },
  4: { text: '已退款', color: 'blue' },
  5: { text: '已发货', color: 'geekblue' },
  6: { text: '已完成', color: 'green' }
};

export default function AdminOrders() {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(false);
  const [shipModalOpen, setShipModalOpen] = useState(false);
  const [shipOrderId, setShipOrderId] = useState(null);
  const [trackingNo, setTrackingNo] = useState('');
  const [shipping, setShipping] = useState(false);

  const load = () => {
    setLoading(true);
    getAdminOrders()
      .then(setOrders)
      .catch(() => {})
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, []);

  const openShip = (order) => {
    setShipOrderId(order.id);
    setTrackingNo('');
    setShipModalOpen(true);
  };

  const submitShip = async () => {
    setShipping(true);
    try {
      await shipOrder(shipOrderId, trackingNo || undefined);
      message.success('已发货');
      setShipModalOpen(false);
      load();
    } catch (e) {
      // 拦截器已提示
    } finally {
      setShipping(false);
    }
  };

  const columns = [
    { title: '订单号', dataIndex: 'orderNo' },
    { title: '金额', dataIndex: 'payAmount', render: (v) => `¥${v}` },
    {
      title: '状态', dataIndex: 'status',
      render: (v) => {
        const s = STATUS_MAP[v] || { text: '未知', color: 'default' };
        return <Tag color={s.color}>{s.text}</Tag>;
      }
    },
    { title: '收货人', dataIndex: 'receiverName' },
    { title: '联系电话', dataIndex: 'receiverPhone' },
    { title: '物流单号', dataIndex: 'trackingNo', render: (v) => v || '-' },
    {
      title: '操作',
      render: (_, order) => (
        order.status === 1 ? (
          <Button type="primary" size="small" onClick={() => openShip(order)}>发货</Button>
        ) : null
      )
    }
  ];

  return (
    <div className="page-container">
      <Card title="订单管理">
        <Table rowKey="id" columns={columns} dataSource={orders} loading={loading} pagination={{ pageSize: 10 }} />
      </Card>

      <Modal
        title="订单发货"
        open={shipModalOpen}
        onOk={submitShip}
        onCancel={() => setShipModalOpen(false)}
        confirmLoading={shipping}
        okText="确认发货"
      >
        <p>请输入物流单号（选填）</p>
        <Input placeholder="物流单号" value={trackingNo} onChange={(e) => setTrackingNo(e.target.value)} />
      </Modal>
    </div>
  );
}