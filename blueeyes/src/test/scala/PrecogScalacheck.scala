package blueeyes

import org.scalacheck._, Gen._
import scala.collection.mutable.Builder
import scalaz._
import java.math.MathContext, MathContext._
import PrecogScalacheck._

object PrecogSpecs {
  import org.specs2._, execute._, matcher._
  import mutable.SpecificationLike

  type ScalaCheck         = org.specs2.ScalaCheck
  val SpecsFailure        = org.specs2.execute.Failure
  type MatchResult[+A]    = org.specs2.matcher.MatchResult[A]
  type SpecsFailure       = org.specs2.execute.Failure
  type SpecsResult        = org.specs2.execute.Result
  type Specification      = org.specs2.mutable.Specification
  type ThrownExpectations = org.specs2.matcher.ThrownExpectations

  trait SpecificationHelp extends SpecificationLike with ThrownMessages {
    def haveAllElementsLike[A](pf: PartialFunction[A, Boolean]): ContainWithResult[A] = contain(like[A]({ case x if pf.isDefinedAt(x) && pf(x) => ok })).forall
  }
}

object PrecogArb {
  implicit def arbBigDecimal: Arbitrary[BigDecimal] = Arbitrary(genBigDecimal)
}

object PrecogScalacheck {
  type Buildable[Elem, Result] = org.scalacheck.util.Buildable[Elem, Result]
  val Pretty                   = org.scalacheck.util.Pretty
  type Pretty                  = org.scalacheck.util.Pretty

  def maxSequenceLength = 16

  implicit def liftGenerator[A](g: Gen[A]): Arbitrary[A] = Arbitrary(g)

  implicit def buildableVector[A] : Buildable[A, Vector[A]] = new Buildable[A, Vector[A]] {
    def builder: Builder[A, Vector[A]] = Vector.newBuilder[A]
  }

  def genFile: Gen[File] = listOfN(3, identifier map (_ take 5)) map (xs => new File(xs.mkString("/", "/", ".cooked")))

  def containerOfAtMostN[C[X] <: Traversable[X], A](maxSize: Int, g: Gen[A])(implicit b: Buildable[A, C[A]]): Gen[C[A]] =
    sized(size => for(n <- choose(0, size min maxSize); c <- containerOfN[C, A](n, g)) yield c)

  def arrayOf[A: ClassManifest](gen: Gen[A]): Gen[Array[A]] = vectorOf(gen) ^^ (_.toArray)
  def vectorOf[A](gen: Gen[A]): Gen[Vector[A]]              = containerOfAtMostN[Vector, A](maxSequenceLength, gen)
  def listOf[A](gen: Gen[A]): Gen[List[A]]                  = containerOfAtMostN[List, A](maxSequenceLength, gen)
  def setOf[A](gen: Gen[A]): Gen[Set[A]]                    = containerOfAtMostN[Set, A](maxSequenceLength, gen)

  def arrayOfN[A: ClassManifest](len: Int, gen: Gen[A]): Gen[Array[A]] = vectorOfN(len, gen) ^^ (_.toArray)
  def vectorOfN[A](len: Int, gen: Gen[A]): Gen[Vector[A]]              = containerOfN[Vector, A](len, gen)
  def setOfN[A](len: Int, gen: Gen[A]): Gen[Set[A]]                    = containerOfN[Set, A](len, gen)

  def genIndex(size: Int): Gen[Int] = Gen.choose(0, size - 1)
  def genBool: Gen[Boolean]         = Arbitrary.arbBool.arbitrary
  def genInt: Gen[Int]              = Arbitrary.arbInt.arbitrary
  def genLong: Gen[Long]            = Arbitrary.arbLong.arbitrary
  def genDouble: Gen[Double]        = Arbitrary.arbDouble.arbitrary
  def genBigInt: Gen[BigInt]        = Arbitrary.arbBigInt.arbitrary
  def genString: Gen[String]        = Arbitrary.arbString.arbitrary
  def genIdentifier: Gen[String]    = Gen.identifier filter (_ != null)
  def genPosLong: Gen[Long]         = choose(1L, Long.MaxValue)
  def genPosInt: Gen[Int]           = choose(1, Int.MaxValue)

  def genBigDecimal: Gen[BigDecimal] = Arbitrary.arbBigDecimal.arbitrary map (d => BigDecimal(d.bigDecimal, d.mc))

  implicit class ArbitraryOps[A](arb: Arbitrary[A]) {
    def ^^[B](f: A => B): Arbitrary[B]      = Arbitrary(arb.arbitrary map f)
    def >>[B](f: A => Gen[B]): Arbitrary[B] = Arbitrary(arb.arbitrary flatMap f)
  }
  implicit class ScalacheckGenOps[A](gen: Gen[A]) {
    def ^^[B](f: A => B): Gen[B]      = gen map f
    def >>[B](f: A => Gen[B]): Gen[B] = gen flatMap f

    def list: Gen[List[A]]       = listOf(gen)
    def optional: Gen[Option[A]] = frequency(
      1  -> None,
      10 -> (gen map (x => Some(x)))
    )
  }
  implicit class ScalacheckGen2Ops[A, B](gen: (Gen[A], Gen[B])) {
    def >>[C](f: (A, B) => C): Gen[C] = for (a <- gen._1 ; b <- gen._2) yield f(a, b)
  }
}
