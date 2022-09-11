package core.mvc.tobe;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import core.di.factory.BeanFactory;
import core.di.factory.example.JdbcUserRepository;
import core.di.factory.example.MyJdbcTemplate;
import javax.sql.DataSource;
import next.config.MyConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ConfigClassPathBeanScannerTest {

    @DisplayName("@Configuration 클래스 내의 @Bean 메서드로 빈을 생성과, classPath 스캔 방식의 빈 생성을 통합한다")
    @Test
    void register_classpathBeanScanner_통합() {
        BeanFactory beanFactory = new BeanFactory();

        ConfigurationBeanScanner configurationBeanScanner = new ConfigurationBeanScanner(beanFactory);
        configurationBeanScanner.register(MyConfiguration.class);

        ClassPathBeanScanner cbds = new ClassPathBeanScanner(beanFactory);
        cbds.doScan("core.di");

        beanFactory.initialize();

        assertNotNull(beanFactory.getBean(DataSource.class));

        JdbcUserRepository userRepository = beanFactory.getBean(JdbcUserRepository.class);
        assertNotNull(userRepository);
        assertNotNull(userRepository.getDataSource());

        MyJdbcTemplate jdbcTemplate = beanFactory.getBean(MyJdbcTemplate.class);
        assertNotNull(jdbcTemplate);
        assertNotNull(jdbcTemplate.getDataSource());
    }

    @Test
    void register_simple() {
        BeanFactory beanFactory = new BeanFactory();
        ConfigurationBeanScanner configBeanScanner = new ConfigurationBeanScanner(beanFactory);
        configBeanScanner.register(MyConfiguration.class);
        beanFactory.initialize();

        assertNotNull(beanFactory.getBean(DataSource.class));
    }

}
