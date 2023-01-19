package boot;

import boot.entity.Member;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.orm.jpa.EntityManagerProxy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.util.concurrent.CountDownLatch;

@SpringBootApplication
@Component
public class SpringBootEntityManagerTest extends Thread {
    private static ApplicationContext context;

    // synchronized 적용이 가능하고 스레드 세이프한 버퍼이다.
    private static StringBuffer sb;

    private static int memberId0CodeInMain;

    // 출력을 위한 도구
    // 모든 스레드가 종료되면 출력해야해서 사용하였다.
    private static CountDownLatch cl;

    private Long id; // 스레드 구분을 위해 주입받는 인덱스(아이디)

    public static void main(String[] args) throws InterruptedException {
        SpringApplication.run(SpringBootEntityManagerTest.class, args);     // 컨텍스트 생성

        // 컨텍스트를 받아온다.
        // ApplicationContextProvider는
        // ApplicationContextAware를 구현하여 직접 만들어야한다.
        context = ApplicationContextProvider.getApplicationContext();

        sb = new StringBuffer();

        // ---> 영속성 컨텍스트를 확인하기위한 멤버 등록
        EntityManager em  = context.getBean(EntityManager.class);
        Member member = new Member();
        member.setId(0L);

        //// 트랜잭션 적용
        PlatformTransactionManager pm = context.getBean(PlatformTransactionManager.class);
        TransactionStatus tx = pm.getTransaction(new DefaultTransactionDefinition());

        try {
            em.persist(member);
            Member findMember = em.find(Member.class, member.getId());
            memberId0CodeInMain = findMember.hashCode();
            pm.commit(tx);
        } catch (Exception e) {
            e.printStackTrace();
            pm.rollback(tx);
        }
        // <--- 멤버 등록 완료

        // 10의 카운트를 생성한다.
        // 각각의 스레드에서 마지막에 cl.countDown()을 호출하여 종료를 알린다.
        cl = new CountDownLatch(10);

        for (Long i = 0L; i < 10; ++i) {
            // 현재의 테스트 클래스를 Thread로 바로 구현하였다.
            SpringBootEntityManagerTest t = new SpringBootEntityManagerTest();
            // 인덱스을 생성자 주입으로 넣을 수가 없어서 setter로 주입하였다.
            t.setId(i + 1);

            t.start();
        }

        // 스레드가 모두 종료, 즉, count가 0이 될 때까지 기다린다.
        cl.await();

        // 출력
        System.out.println(sb);
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    @Transactional
    public void run() {
        // 이상하게 @Transactional이 동작하지 않아서 스프링 프레임워크의 방식으로 적용했다.
        PlatformTransactionManager pm = context.getBean(PlatformTransactionManager.class);
        TransactionStatus tx = pm.getTransaction(new DefaultTransactionDefinition());

        // 이제부터 조금 헷갈린다.
        // EM을 어떤 방식으로 가져왔는지 잘 봐야한다.
        // 연속적으로 가지고 오는 경우도 확인했다.
        try {
            // 컨텍스트에서 가져온 EMF
            EntityManagerFactory emf = context.getBean(EntityManagerFactory.class);

            // 1. 컨텍스트에서 가져온 EM
            EntityManager emFromContext1 = context.getBean(EntityManager.class);
            EntityManager emFromContext2 = context.getBean(EntityManager.class); // 연속 호출

            // 2. 컨텍스트의 EMF에서 가져온 EM
            EntityManager emFromEmf1 = emf.createEntityManager();
            EntityManager emFromEmf2 = emf.createEntityManager();

            // 3. 컨텍스트에서 가져온 EM의 실제 Target EM
            EntityManager emFromContext1_Target = ((EntityManagerProxy)emFromContext1).getTargetEntityManager();
            EntityManager emFromContext2_Target = ((EntityManagerProxy)emFromContext2).getTargetEntityManager();

            // 4. 컨텍스트 EMF에서 가져온 EM의 실제 Target EM
            EntityManager emFromEmf1_Target = ((EntityManagerProxy)emFromEmf1).getTargetEntityManager();
            EntityManager emFromEmf2_Target = ((EntityManagerProxy)emFromEmf2).getTargetEntityManager();

            // 컨텍스트 EM의 영속성 컨텍스트 공유 확인
            Member member = emFromContext1.find(Member.class, 0L);

            // 출력 부분
            synchronized (sb) {
                sb.append(String.format("=========== Thread <%d> ===========\n", id));

                sb.append(String.format("Context에서 가져온 EMF: %s(%s)\n\n", emf.getClass(), emf.hashCode()));

                sb.append(String.format("1. Context에서 가져온 EM: %s(%s)\n", emFromContext1.getClass(), emFromContext1.hashCode()));
                sb.append(String.format("EM 연속 호출 시 같은 객체인가? > %s\n\n", emFromContext1 == emFromContext2));

                sb.append(String.format("2. EMF로부터 생성한 EM: %s(%s)\n", emFromEmf1.getClass(), emFromEmf1.hashCode()));
                sb.append(String.format("EM 연속 호출 시 같은 객체인가? > %s\n\n", emFromEmf1 == emFromEmf2));

                sb.append(String.format("3. 컨텍스트로부터 생성한 EM의 실제 타겟: %s(%s)\n", emFromContext1_Target.getClass(), emFromContext1_Target.hashCode()));
                sb.append(String.format("재호출 : %s(%s)\n", emFromContext2_Target.getClass(), emFromContext2_Target.hashCode()));
                sb.append(String.format("컨텍스트로부터 생성한 EM의 타겟은 항상 같은가? > %s\n\n", emFromContext1_Target == emFromContext2_Target));

                sb.append(String.format("4. EMF로부터 생성한 EM의 실제 타겟: %s(%s)\n", emFromEmf1_Target.getClass(), emFromEmf1_Target.hashCode()));
                sb.append(String.format("재호출 : %s(%s)\n", emFromEmf2_Target.getClass(), emFromEmf2_Target.hashCode()));
                sb.append(String.format("EMF부터 생성한 EM의 타깃은 항상 같은가? > %s\n\n", emFromEmf1_Target == emFromEmf2_Target));

                sb.append(String.format("Main: %d, Cur: %d\n", memberId0CodeInMain, member.hashCode()));
                sb.append(String.format("===================================\n"));
            }

            pm.commit(tx);
        } catch (Exception e) {
            e.printStackTrace();
            pm.rollback(tx);
        } finally {
            // 종료하기 전 카운트다운한다.
            cl.countDown();
        }
    }
}
