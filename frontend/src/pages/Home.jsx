import React, { useEffect, useState } from 'react';
import { Row, Col, Card, Tabs, Select, Empty, Pagination, Spin } from 'antd';
import { useSearchParams } from 'react-router-dom';
import ProductCard from '../components/ProductCard.jsx';
import { getCategories, searchProducts, getHotProducts } from '../api/index.js';

export default function Home() {
  const [searchParams, setSearchParams] = useSearchParams();
  const keyword = searchParams.get('keyword') || '';

  const [categories, setCategories] = useState([]);
  const [categoryId, setCategoryId] = useState(undefined);
  const [sort, setSort] = useState('sales');
  const [page, setPage] = useState(1);
  const [size] = useState(8);
  const [data, setData] = useState({ total: 0, page: 1, size: 8, records: [] });
  const [hot, setHot] = useState([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    getCategories().then(setCategories).catch(() => {});
    getHotProducts(8).then(setHot).catch(() => {});
  }, []);

  useEffect(() => {
    setLoading(true);
    searchProducts({ keyword, categoryId, page, size, sort })
      .then(setData)
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [keyword, categoryId, sort, page]);

  const categoryItems = [
    { key: 'all', label: '全部' },
    ...categories.filter((c) => c.parentId === 0).map((c) => ({ key: String(c.id), label: c.name }))
  ];

  const onCategoryChange = (key) => {
    setCategoryId(key === 'all' ? undefined : Number(key));
    setPage(1);
  };

  const onSortChange = (value) => {
    setSort(value);
    setPage(1);
  };

  return (
    <div className="page-container">
      {hot.length > 0 && keyword === '' && (
        <Card title="热销推荐" style={{ marginBottom: 16 }}>
          <Row gutter={[16, 16]}>
            {hot.map((p) => (
              <Col xs={12} sm={8} md={6} key={p.id}>
                <ProductCard product={p} />
              </Col>
            ))}
          </Row>
        </Card>
      )}

      <Card>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
          <Tabs
            activeKey={categoryId === undefined ? 'all' : String(categoryId)}
            items={categoryItems}
            onChange={onCategoryChange}
          />
          <Select
            value={sort}
            style={{ width: 160 }}
            onChange={onSortChange}
            options={[
              { value: 'sales', label: '按销量' },
              { value: 'price_asc', label: '价格从低到高' },
              { value: 'price_desc', label: '价格从高到低' }
            ]}
          />
        </div>

        <Spin spinning={loading}>
          {data.records.length === 0 ? (
            <Empty description="暂无商品" />
          ) : (
            <Row gutter={[16, 16]}>
              {data.records.map((p) => (
                <Col xs={12} sm={8} md={6} key={p.id}>
                  <ProductCard product={p} />
                </Col>
              ))}
            </Row>
          )}
        </Spin>

        <div style={{ textAlign: 'center', marginTop: 16 }}>
          <Pagination
            current={page}
            pageSize={size}
            total={data.total}
            onChange={setPage}
            showTotal={(total) => `共 ${total} 件商品`}
          />
        </div>
      </Card>
    </div>
  );
}