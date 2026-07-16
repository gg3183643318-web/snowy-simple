package com.example.snowydemo.plugin.crm.service;

import cn.dev33.satoken.stp.StpUtil;
import com.example.snowydemo.common.CommonException;
import com.example.snowydemo.common.annotation.DemoLog;
import com.example.snowydemo.plugin.api.crm.CustomerCreateRequest;
import com.example.snowydemo.plugin.api.crm.CustomerDTO;
import com.example.snowydemo.plugin.api.crm.CustomerUpdateRequest;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * CRM 客户业务的内存实现。
 *
 * <p>真实项目通常会在这里调用 Mapper，再由 Mapper 访问 MySQL。本示例暂时用
 * {@link LinkedHashMap} 替代 Mapper/MySQL，只是为了隔离并突出 HTTP 请求方法和权限校验的学习重点。
 * 数据只存在当前 Java 进程的内存中，所以应用一旦重启，新增和修改的数据都会丢失，并重新恢复为
 * 构造方法中的 1001-1003 三条初始客户。</p>
 *
 * <p>Spring MVC 会让多个 Tomcat 请求线程共同调用这个单例 Service，因此所有读写方法都使用
 * {@code synchronized}。它让一次请求完整操作完 Map 后，另一个请求才能进入，避免教学演示中出现
 * 读写交叉造成的不确定结果；这是一种简单的演示方案，不代表生产项目应放弃数据库事务。</p>
 *
 * <p>{@link DemoLog} 只是贴在方法上的被动元数据，本身不会打印日志。后续 AOP 会读取这些注解，
 * 在不修改业务逻辑的前提下观察 Service 调用。</p>
 */
@Service
public class CrmCustomerService {

    private final LinkedHashMap<Long, CustomerDTO> customers = new LinkedHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1004L);

    public CrmCustomerService() {
        customers.put(1001L, new CustomerDTO(1001L, "上海星河科技", "李经理", "A"));
        customers.put(1002L, new CustomerDTO(1002L, "杭州云启制造", "王经理", "B"));
        customers.put(1003L, new CustomerDTO(1003L, "深圳青木贸易", "赵经理", "C"));
    }

    /**
     * 按插入顺序查询客户，并返回不可修改的快照；调用方不能绕过 Service 改动内部 Map。
     */
    @DemoLog("查询客户")
    public synchronized List<CustomerDTO> listCustomers() {
        return List.copyOf(customers.values());
    }

    /**
     * 生成新 ID 后保存客户。{@code synchronized} 保证并发请求不会交叉修改有序 Map。
     */
    @DemoLog("新增客户")
    public synchronized CustomerDTO create(CustomerCreateRequest request) {
        long id = nextId.getAndIncrement();
        CustomerDTO customer = new CustomerDTO(id, request.name(), request.owner(), request.level());
        customers.put(id, customer);
        return customer;
    }

    /**
     * 修改接口故意把手动权限检查放在 Service，而不是在 Controller 使用权限注解。
     * 这样可以和查询、新增、删除的注解验权形成对照：请求先进入 Controller，Controller 调用
     * Service 后才执行这里的 {@link StpUtil#checkPermission(String)}，通过后才会修改数据。
     */
    @DemoLog("修改客户（Service 手动验权）")
    public synchronized CustomerDTO update(Long id, CustomerUpdateRequest request) {
        StpUtil.checkPermission("crm:customer:update");
        if (!customers.containsKey(id)) {
            throw new CommonException(404, "客户不存在");
        }

        CustomerDTO customer = new CustomerDTO(id, request.name(), request.owner(), request.level());
        customers.put(id, customer);
        return customer;
    }

    /**
     * 删除路径中指定的客户；不存在时抛出统一业务异常，由 web-app 转成 404 JSON 响应。
     */
    @DemoLog("删除客户")
    public synchronized void delete(Long id) {
        if (!customers.containsKey(id)) {
            throw new CommonException(404, "客户不存在");
        }
        customers.remove(id);
    }
}
