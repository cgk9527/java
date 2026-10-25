import React, { useEffect, useState } from 'react';
import {
  Card, Table, Button, Modal, Form, Input, InputNumber, Select,
  Space, Drawer, Popconfirm, Tag, message
} from 'antd';
import {
  getAdminProducts, createAdminProduct, updateAdminProduct, updateAdminProductStatus,
  getAdminSkus, addAdminSku, updateAdminSku, deleteAdminSku, getCategories
} from '../api/index.js';

export default function Admin() {
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [loading, setLoading] = useState(false);

  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState(null);
  const [productForm] = Form.useForm();

  const [skuOpen, setSkuOpen] = useState(false);
  const [currentProduct, setCurrentProduct] = useState(null);
  const [skus, setSkus] = useState([]);
  const [editingSku, setEditingSku] = useState(null);
  const [skuForm] = Form.useForm();

  const load = () => {
    setLoading(true);
    getAdminProducts()
      .then(setProducts)
      .catch(() => {})
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    load();
    getCategories().then(setCategories).catch(() => {});
  }, []);

  const categoryName = (id) => {
    const c = categories.find((x) => x.id === id);
    return c ? c.name : id;
  };

  const openCreate = () => {
    setEditing(null);
    productForm.resetFields();
    setModalOpen(true);
  };

  const openEdit = (record) => {
    setEditing(record);
    productForm.setFieldsValue(record);
    setModalOpen(true);
  };

  const submitProduct = async () => {
    const values = await productForm.validateFields();
    if (editing) {
      await updateAdminProduct(editing.id, values);
      message.success('已更新商品');
    } else {
      await createAdminProduct(values);
      message.success('已新增商品(请进入 SKU 管理补充规格)');
    }
    setModalOpen(false);
    load();
  };

  const openSkus = async (record) => {
    setCurrentProduct(record);
    setEditingSku(null);
    skuForm.resetFields();
    setSkuOpen(true);
    const list = await getAdminSkus(record.id).catch(() => []);
    setSkus(list || []);
  };

  const submitSku = async () => {
    const values = await skuForm.validateFields();
    if (editingSku) {
      await updateAdminSku(editingSku.id, { specs: values.specs, skuCode: values.skuCode, price: values.price, stock: values.stock });
      message.success('已更新 SKU');
    } else {
      await addAdminSku(currentProduct.id, values);
      message.success('已新增 SKU');
    }
    setEditingSku(null);
    skuForm.resetFields();
    const list = await getAdminSkus(currentProduct.id).catch(() => []);
    setSkus(list || []);
  };

  const columns = [
    { title: 'ID', dataIndex: 'id', width: 60 },
    { title: '商品名称', dataIndex: 'name' },
    { title: '分类', dataIndex: 'categoryId', render: categoryName },
    { title: '价格', dataIndex: 'price', render: (v) => `¥${v}` },
    { title: '销量', dataIndex: 'sales' },
    {
      title: '状态', dataIndex: 'status',
      render: (v) => (v === 1 ? <Tag color="green">上架</Tag> : <Tag>下架</Tag>)
    },
    {
      title: '操作',
      render: (_, record) => (
        <Space>
          <Button size="small" onClick={() => openEdit(record)}>编辑</Button>
          <Button size="small" onClick={() => openSkus(record)}>SKU管理</Button>
          {record.status === 1 ? (
            <Popconfirm title="确认下架该商品?" onConfirm={() => onToggleStatus(record, 0)}>
              <Button size="small" danger>下架</Button>
            </Popconfirm>
          ) : (
            <Button size="small" type="primary" onClick={() => onToggleStatus(record, 1)}>上架</Button>
          )}
        </Space>
      )
    }
  ];

  const onToggleStatus = async (record, status) => {
    await updateAdminProductStatus(record.id, status);
    message.success(status === 1 ? '已上架' : '已下架');
    load();
  };

  return (
    <div className="page-container">
      <Card
        title="商品后台管理"
        extra={<Button type="primary" onClick={openCreate}>新增商品</Button>}
      >
        <Table rowKey="id" columns={columns} dataSource={products} loading={loading} pagination={{ pageSize: 10 }} />
      </Card>

      <Modal
        title={editing ? '编辑商品' : '新增商品'}
        open={modalOpen}
        onOk={submitProduct}
        onCancel={() => setModalOpen(false)}
        okText="保存"
      >
        <Form form={productForm} layout="vertical">
          <Form.Item name="name" label="商品名称" rules={[{ required: true, message: '请输入商品名称' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="categoryId" label="分类" rules={[{ required: true, message: '请选择分类' }]}>
            <Select options={categories.filter((c) => c.parentId === 0).map((c) => ({ value: c.id, label: c.name }))} />
          </Form.Item>
          <Form.Item name="subtitle" label="副标题">
            <Input />
          </Form.Item>
          <Form.Item name="mainImage" label="主图 URL">
            <Input />
          </Form.Item>
          <Form.Item name="price" label="价格" rules={[{ required: true, message: '请输入价格' }]}>
            <InputNumber min={0.01} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="detail" label="商品详情">
            <Input.TextArea rows={3} />
          </Form.Item>
        </Form>
      </Modal>

      <Drawer
        title={`SKU 管理 - ${currentProduct?.name || ''}`}
        open={skuOpen}
        onClose={() => setSkuOpen(false)}
        width={560}
      >
        <Form form={skuForm} layout="inline" style={{ marginBottom: 16 }}>
          <Form.Item name="skuCode" label="编码" rules={[{ required: true, message: '必填' }]}>
            <Input style={{ width: 140 }} />
          </Form.Item>
          <Form.Item name="specs" label="规格">
            <Input style={{ width: 140 }} />
          </Form.Item>
          <Form.Item name="price" label="价格" rules={[{ required: true, message: '必填' }]}>
            <InputNumber min={0.01} style={{ width: 90 }} />
          </Form.Item>
          <Form.Item name="stock" label="库存" rules={[{ required: true, message: '必填' }]}>
            <InputNumber min={0} style={{ width: 80 }} />
          </Form.Item>
          <Button type="primary" onClick={submitSku}>{editingSku ? '更新' : '新增'}</Button>
          {editingSku && <Button onClick={() => { setEditingSku(null); skuForm.resetFields(); }}>取消</Button>}
        </Form>

        <Table
          rowKey="id"
          dataSource={skus}
          pagination={false}
          columns={[
            { title: '编码', dataIndex: 'skuCode' },
            { title: '规格', dataIndex: 'specs' },
            { title: '价格', dataIndex: 'price', render: (v) => `¥${v}` },
            { title: '库存', dataIndex: 'stock' },
            {
              title: '状态', dataIndex: 'status',
              render: (v) => (v === 1 ? <Tag color="green">在售</Tag> : <Tag>停用</Tag>)
            },
            {
              title: '操作',
              render: (_, record) => (
                <Space>
                  <Button size="small" onClick={() => {
                    setEditingSku(record);
                    skuForm.setFieldsValue(record);
                  }}>编辑</Button>
                  {record.status === 1 && (
                    <Popconfirm title="确认停用该 SKU?" onConfirm={async () => {
                      await deleteAdminSku(record.id);
                      message.success('已停用');
                      const list = await getAdminSkus(currentProduct.id).catch(() => []);
                      setSkus(list || []);
                    }}>
                      <Button size="small" danger>停用</Button>
                    </Popconfirm>
                  )}
                </Space>
              )
            }
          ]}
        />
      </Drawer>
    </div>
  );
}