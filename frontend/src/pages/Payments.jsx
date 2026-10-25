import React, { useEffect, useState } from 'react';
import { Card, Table, Tag, Button, Modal, Input, message, Empty } from 'antd';
import { getPayments, refundOrder } from '../api/index.js';

const CHANNEL_MAP = {
  mock: '模拟支付',
  alipay: '支付宝网页',
  alipay_qr: '支付宝扫码',
  wechat: '微信扫码'
};

const PAY_STATUS_MAP = {
  0: { text: '待支付', color: 'orange' },
  1: { text: '已支付', color: 'green' },
  2: { text: '支付失败', color: 'red' }
};

const REFUND_STATUS_MAP = {
  0: { text: '退款处理中', color: 'orange' },
  1: { text: '已退款', color: 'green' },
  2: { text: '退款失败', color: 'red' }
};

export default function Payments() {
  const [payments, setPayments] = useState([]);
  const [loading, setLoading] = useState(false);
  const [modalOpen, setModalOpen] = useState(false);
  const [current, setCurrent] = useState(null);
  const [reason, setReason] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const load = () => {
    setLoading(true);
    getPayments()
      .then(setPayments)
      .catch(() => {})
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
  }, []);

  const openRefund = (payment) => {
    setCurrent(payment);
    setReason('');
    setModalOpen(true);
  };

  const submitRefund = async () => {
    setSubmitting(true);
    try {
      await refundOrder(current.orderId, reason || '申请退款');
      message.success('退款已受理');
      setModalOpen(false);
      load();
    } catch (e) {
      // 拦截器已提示
    } finally {
      setSubmitting(false);
    }
  };

  const columns = [
    { title: '订单号', dataIndex: 'orderNo' },
    { title: '金额', dataIndex: 'amount', render: (v) => `¥${v}` },
    { title: '渠道', dataIndex: 'channel', render: (v) => CHANNEL_MAP[v] || v || '-' },
    {
      title: '支付状态',
      dataIndex: 'status',
      render: (v) => {
        const s = PAY_STATUS_MAP[v] || { text: '未知', color: 'default' };
        return <Tag color={s.color}>{s.text}</Tag>;
      }
    },
    { title: '交易号', dataIndex: 'tradeNo', render: (v) => v || '-' },
    { title: '支付时间', dataIndex: 'payTime', render: (v) => v || '-' },
    {
      title: '退款',
      dataIndex: 'refund',
      render: (refund) => {
        if (!refund) return <span style={{ color: '#999' }}>无</span>;
        const s = REFUND_STATUS_MAP[refund.status] || { text: '未知', color: 'default' };
        return (
          <div>
            <Tag color={s.color}>{s.text}</Tag>
            <div style={{ color: '#999', fontSize: 12 }}>{refund.refundNo}</div>
          </div>
        );
      }
    },
    {
      title: '操作',
      render: (_, payment) => {
        const canRefund = payment.status === 1 && (!payment.refund || payment.refund.status === 2);
        return canRefund ? (
          <Button type="primary" danger size="small" onClick={() => openRefund(payment)}>申请退款</Button>
        ) : null;
      }
    }
  ];

  return (
    <div className="page-container">
      <Card title="支付流水">
        <Table
          rowKey="paymentId"
          columns={columns}
          dataSource={payments}
          loading={loading}
          pagination={{ pageSize: 10 }}
          locale={{ emptyText: <Empty description="暂无支付记录" /> }}
        />
      </Card>

      <Modal
        title="申请退款"
        open={modalOpen}
        onOk={submitRefund}
        onCancel={() => setModalOpen(false)}
        confirmLoading={submitting}
        okText="确认退款"
      >
        <p>订单号：{current?.orderNo}</p>
        <p>退款金额：¥{current?.amount}</p>
        <Input.TextArea
          rows={3}
          placeholder="退款原因（选填）"
          value={reason}
          onChange={(e) => setReason(e.target.value)}
        />
      </Modal>
    </div>
  );
}