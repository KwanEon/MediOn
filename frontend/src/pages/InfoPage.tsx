interface InfoPageProps {
  eyebrow: string;
  title: string;
  description: string;
}

function InfoPage({ eyebrow, title, description }: InfoPageProps) {
  return (
    <main className="main-content info-page">
      <section className="info-page-content">
        <p>{eyebrow}</p>
        <h1>{title}</h1>
        <span>{description}</span>
      </section>
    </main>
  );
}

export default InfoPage;
