package hiber;

import boot.entity.Member;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.concurrent.CountDownLatch;

public class PersistenceTest extends Thread {
    private int id;
    private static int memberId0CodeInMain;
    private static EntityManagerFactory emf;

    private static StringBuffer sb;

    private static CountDownLatch cl;

    public PersistenceTest(int id) {
        this.id = id;
    }

    public static void main(String[] args) throws InterruptedException {
        emf = Persistence.createEntityManagerFactory("hello");
        sb = new StringBuffer();
        cl = new CountDownLatch(10);

        Member member = new Member();
        member.setId(0L);

        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            em.persist(member);
            Member findMember = em.find(Member.class, member.getId());
            memberId0CodeInMain = findMember.hashCode();
            em.getTransaction().commit();
        } catch (Exception e) {
            e.printStackTrace();
            em.getTransaction().rollback();
        }


        for (int i = 0; i < 10; ++i) {
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
            Member findMember = em.find(Member.class, 0L);

            synchronized(sb) {
                sb.append(String.format("=== <%d> ===\n", id));
                sb.append(String.format("EM: %s(%d)\nMain:  %d\nThread: %d\n",
                        em.getClass(), em.hashCode(), memberId0CodeInMain, findMember.hashCode()));
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
