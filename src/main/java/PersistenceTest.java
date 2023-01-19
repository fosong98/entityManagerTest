import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.concurrent.CountDownLatch;

public class PersistenceTest extends Thread {
    private static EntityManagerFactory emf;
    private int id;

    private static StringBuffer sb;

    private static CountDownLatch cl;

    public PersistenceTest(int id) {
        this.id = id;
    }

    public static void main(String[] args) throws InterruptedException {
        emf = Persistence.createEntityManagerFactory("hello");
        sb = new StringBuffer();
        cl = new CountDownLatch(100);

        for (int i = 0; i < 100; ++i) {
            PersistenceTest t = new PersistenceTest(i + 1);
            t.start();
        }

        cl.await();

        System.out.println(sb);
    }

    @Override
    public void run() {
        EntityManager em = emf.createEntityManager();

        try {
            em.getTransaction().begin();
            Member member = new Member();
            member.setId((long) id);

            em.persist(member);
            Member findMember = em.find(Member.class, member.getId());

            synchronized(sb) {
                sb.append(String.format("=== <%d> ===\n", id));
                sb.append(String.format("EM: %s, %s, %s\n", em, member, findMember));
                sb.append(String.format("==============\n"));
            };

            em.getTransaction().commit();
        } catch (Exception e) {
            em.getTransaction().rollback();
            e.printStackTrace();
        } finally {
            em.close();
            cl.countDown();
        }
    }
}
