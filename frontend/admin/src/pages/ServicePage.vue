<template>
  <AppShell title="服务管理" subtitle="当前保留只读说明">
    <div class="page-stack">
      <section class="readonly-card">
        <div class="section-heading">
          <div>
            <span class="inline-tag">只读</span>
            <h3 class="section-title">服务配置暂不开放写入</h3>
            <p class="section-copy">当前仓库未发现与这页完整匹配的后台配置接口，所以这里不再保留假表单、假保存和假删除操作。</p>
          </div>
        </div>

        <div class="readonly-list">
          <article class="readonly-item">
            <h4 class="readonly-title">本轮已处理</h4>
            <p class="readonly-copy">移除了原型态的套餐配置、删除确认、成功提示和 uni 运行时调用，避免页面误导为“已经可用”。</p>
          </article>
          <article class="readonly-item">
            <h4 class="readonly-title">后续接入建议</h4>
            <p class="readonly-copy">等后台明确提供服务项、套餐、价格或服务范围配置接口后，再把这页恢复成真实配置台。</p>
          </article>
          <article class="readonly-item">
            <h4 class="readonly-title">当前用途</h4>
            <p class="readonly-copy">用于说明能力边界，告诉运营同学这块尚未接管到真实后台，不会出现假保存成功但数据没落库的情况。</p>
          </article>
        </div>
      </section>
    </div>
  </AppShell>
</template>

<script setup>
import AppShell from '../components/AppShell.vue'
</script>
